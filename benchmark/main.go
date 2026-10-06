package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"math"
	"net/http"
	"net/url"
	"os"
	"strings"
	"sync"
	"sync/atomic"
	"time"
)

type options struct {
	endpoint    string
	to          string
	rate        int
	duration    time.Duration
	drain       time.Duration
	maxInFlight int
	bodyBytes   int
	timeout     time.Duration
	out         string
	rabbitURL   string
	queue       string
	rabbitUser  string
	rabbitPass  string
}

type requestPayload struct {
	To           string `json:"to"`
	Subject      string `json:"subject"`
	HTMLTemplate string `json:"htmlTemplate"`
}

type rabbitSnapshot struct {
	At                   time.Time `json:"at"`
	Ready                int       `json:"messages_ready"`
	Unacknowledged       int       `json:"messages_unacknowledged"`
	PublishRatePerSecond float64   `json:"publish_rate_per_second"`
	DeliverRatePerSecond float64   `json:"deliver_rate_per_second"`
}

type result struct {
	Endpoint       string           `json:"endpoint"`
	TargetRate     int              `json:"target_requests_per_second"`
	LoadDuration   string           `json:"load_duration"`
	DrainDuration  string           `json:"drain_duration"`
	Scheduled      int64            `json:"scheduled"`
	Accepted       int64            `json:"accepted_202"`
	HTTPFailures   int64            `json:"http_failures"`
	RequestErrors  int64            `json:"request_errors"`
	CapacityDrops  int64            `json:"capacity_drops"`
	ActualRate     float64          `json:"accepted_requests_per_second"`
	Latency        latencySummary   `json:"latency"`
	StatusCodes    map[int]int64    `json:"status_codes"`
	RabbitMQ       []rabbitSnapshot `json:"rabbitmq_samples,omitempty"`
	RabbitMQErrors int64            `json:"rabbitmq_sample_errors,omitempty"`
}

type latencySummary struct {
	Samples  int64         `json:"samples"`
	Average  string        `json:"average"`
	P50      string        `json:"p50_approx"`
	P95      string        `json:"p95_approx"`
	P99      string        `json:"p99_approx"`
	Max      string        `json:"max"`
	Min      string        `json:"min"`
	Buckets  [8192]uint64  `json:"-"`
	Sum      time.Duration `json:"-"`
	MinNanos int64         `json:"-"`
	MaxNanos int64         `json:"-"`
}

type counters struct {
	scheduled     atomic.Int64
	accepted      atomic.Int64
	httpFailures  atomic.Int64
	requestErrors atomic.Int64
	drops         atomic.Int64
	mu            sync.Mutex
	statuses      map[int]int64
	latency       latencySummary
}

type queueResponse struct {
	MessagesReady          int `json:"messages_ready"`
	MessagesUnacknowledged int `json:"messages_unacknowledged"`
	MessageStats           struct {
		Publish struct {
			Rate float64 `json:"rate"`
		} `json:"publish_details"`
		Deliver struct {
			Rate float64 `json:"rate"`
		} `json:"deliver_get_details"`
	} `json:"message_stats"`
}

func main() {
	var opt options
	flag.StringVar(&opt.endpoint, "url", "http://localhost:8081/api/email/send", "Publisher email endpoint")
	flag.StringVar(&opt.to, "to", "", "Required recipient address on a test SMTP sink")
	flag.IntVar(&opt.rate, "rate", 100, "Target request arrival rate per second")
	flag.DurationVar(&opt.duration, "duration", 30*time.Second, "Time to send requests")
	flag.DurationVar(&opt.drain, "drain", 30*time.Second, "Additional time to sample RabbitMQ after load stops")
	flag.IntVar(&opt.maxInFlight, "max-inflight", 500, "Maximum concurrent HTTP requests; excess arrivals are counted as drops")
	flag.IntVar(&opt.bodyBytes, "body-bytes", 1024, "Approximate HTML body size in bytes")
	flag.DurationVar(&opt.timeout, "timeout", 15*time.Second, "HTTP request timeout")
	flag.StringVar(&opt.out, "out", "benchmark-results.json", "JSON results path; empty disables file output")
	flag.StringVar(&opt.rabbitURL, "rabbit-url", "", "Optional RabbitMQ Management URL, for example http://localhost:15672")
	flag.StringVar(&opt.queue, "rabbit-queue", "email.queue", "RabbitMQ queue name to sample")
	flag.StringVar(&opt.rabbitUser, "rabbit-user", "guest", "RabbitMQ Management username")
	flag.StringVar(&opt.rabbitPass, "rabbit-password", os.Getenv("PONGPIN_RABBITMQ_PASSWORD"), "RabbitMQ Management password (or set PONGPIN_RABBITMQ_PASSWORD)")
	flag.Parse()

	if err := validate(opt); err != nil {
		fmt.Fprintln(os.Stderr, "configuration error:", err)
		os.Exit(2)
	}
	if opt.rabbitURL != "" && opt.rabbitPass == "" {
		fmt.Fprintln(os.Stderr, "set -rabbit-password or PONGPIN_RABBITMQ_PASSWORD when RabbitMQ sampling is enabled")
		os.Exit(2)
	}

	transport := &http.Transport{MaxIdleConns: opt.maxInFlight, MaxIdleConnsPerHost: opt.maxInFlight}
	client := &http.Client{Timeout: opt.timeout, Transport: transport}
	stats := &counters{statuses: make(map[int]int64)}
	var samples []rabbitSnapshot
	var sampleErrors atomic.Int64
	monitorDone := make(chan struct{})
	monitorStop := make(chan struct{})
	if opt.rabbitURL != "" {
		go monitorRabbit(client, opt, &samples, &sampleErrors, monitorStop, monitorDone)
	} else {
		close(monitorDone)
	}

	body := strings.Repeat("x", opt.bodyBytes)
	semaphore := make(chan struct{}, opt.maxInFlight)
	var requests sync.WaitGroup
	interval := time.Second / time.Duration(opt.rate)
	ticker := time.NewTicker(interval)
	loadTimer := time.NewTimer(opt.duration)
	loadStart := time.Now()
	loadEnd := loadStart.Add(opt.duration)
	fmt.Printf("Sending at %d requests/sec for %s to %s\n", opt.rate, opt.duration, opt.endpoint)
	scheduleRequest(client, opt, body, stats, semaphore, &requests)

loop:
	for {
		select {
		case <-ticker.C:
			if !time.Now().Before(loadEnd) {
				break loop
			}
			scheduleRequest(client, opt, body, stats, semaphore, &requests)
		case <-loadTimer.C:
			break loop
		}
	}
	ticker.Stop()
	loadTimer.Stop()
	loadElapsed := time.Since(loadStart)
	requests.Wait()
	if opt.drain > 0 {
		fmt.Printf("Load complete; sampling drain for %s\n", opt.drain)
		time.Sleep(opt.drain)
	}
	if opt.rabbitURL != "" {
		close(monitorStop)
		<-monitorDone
	}
	transport.CloseIdleConnections()

	accepted := stats.accepted.Load()
	output := result{
		Endpoint: opt.endpoint, TargetRate: opt.rate, LoadDuration: loadElapsed.String(),
		DrainDuration: opt.drain.String(), Scheduled: stats.scheduled.Load(), Accepted: accepted,
		HTTPFailures: stats.httpFailures.Load(), RequestErrors: stats.requestErrors.Load(),
		CapacityDrops: stats.drops.Load(), ActualRate: float64(accepted) / loadElapsed.Seconds(),
		Latency: stats.latencySummary(), StatusCodes: stats.statusCounts(),
		RabbitMQ: samples, RabbitMQErrors: sampleErrors.Load(),
	}
	printResult(output)
	if opt.out != "" {
		if err := writeResult(opt.out, output); err != nil {
			fmt.Fprintln(os.Stderr, "could not write results:", err)
			os.Exit(1)
		}
		fmt.Println("JSON results:", opt.out)
	}
}

func scheduleRequest(client *http.Client, opt options, body string, stats *counters, semaphore chan struct{}, requests *sync.WaitGroup) {
	sequence := stats.scheduled.Add(1)
	select {
	case semaphore <- struct{}{}:
		requests.Add(1)
		go send(client, opt, body, sequence, stats, semaphore, requests)
	default:
		stats.drops.Add(1)
	}
}

func validate(opt options) error {
	if opt.rate < 1 || time.Second/time.Duration(opt.rate) <= 0 {
		return fmt.Errorf("rate must be between 1 and 1,000,000,000 requests/sec")
	}
	if strings.TrimSpace(opt.to) == "" {
		return fmt.Errorf("provide -to with an address accepted by your test SMTP sink")
	}
	if opt.duration <= 0 || opt.drain < 0 || opt.maxInFlight < 1 || opt.bodyBytes < 0 || opt.timeout <= 0 {
		return fmt.Errorf("duration, max-inflight, and timeout must be positive; drain and body-bytes cannot be negative")
	}
	parsed, err := url.ParseRequestURI(opt.endpoint)
	if err != nil || parsed.Scheme == "" || parsed.Host == "" {
		return fmt.Errorf("url must be an absolute HTTP(S) URL")
	}
	if parsed.Scheme != "http" && parsed.Scheme != "https" {
		return fmt.Errorf("url must use HTTP or HTTPS")
	}
	if opt.rabbitURL != "" {
		parsed, err = url.ParseRequestURI(opt.rabbitURL)
		if err != nil || parsed.Scheme == "" || parsed.Host == "" {
			return fmt.Errorf("rabbit-url must be an absolute HTTP(S) URL")
		}
		if parsed.Scheme != "http" && parsed.Scheme != "https" {
			return fmt.Errorf("rabbit-url must use HTTP or HTTPS")
		}
	}
	return nil
}

func send(client *http.Client, opt options, body string, sequence int64, stats *counters, semaphore chan struct{}, requests *sync.WaitGroup) {
	defer requests.Done()
	defer func() { <-semaphore }()
	payload, _ := json.Marshal(requestPayload{
		To: opt.to, Subject: fmt.Sprintf("pongpin-benchmark-%d", sequence), HTMLTemplate: "<p>" + body + "</p>",
	})
	request, err := http.NewRequest(http.MethodPost, opt.endpoint, strings.NewReader(string(payload)))
	if err != nil {
		stats.requestErrors.Add(1)
		return
	}
	request.Header.Set("Content-Type", "application/json")
	started := time.Now()
	response, err := client.Do(request)
	if err != nil {
		stats.recordLatency(time.Since(started))
		stats.requestErrors.Add(1)
		return
	}
	_, _ = io.Copy(io.Discard, response.Body)
	_ = response.Body.Close()
	stats.recordLatency(time.Since(started))
	stats.recordStatus(response.StatusCode)
	if response.StatusCode == http.StatusAccepted {
		stats.accepted.Add(1)
	} else {
		stats.httpFailures.Add(1)
	}
}

func monitorRabbit(client *http.Client, opt options, samples *[]rabbitSnapshot, failures *atomic.Int64, stop <-chan struct{}, done chan<- struct{}) {
	defer close(done)
	defer func() {
		if recovered := recover(); recovered != nil {
			failures.Add(1)
		}
	}()
	ticker := time.NewTicker(time.Second)
	defer ticker.Stop()
	for {
		if snapshot, err := sampleQueue(client, opt); err != nil {
			failures.Add(1)
		} else {
			*samples = append(*samples, snapshot)
		}
		select {
		case <-stop:
			return
		case <-ticker.C:
		}
	}
}

func sampleQueue(client *http.Client, opt options) (rabbitSnapshot, error) {
	base := strings.TrimRight(opt.rabbitURL, "/")
	endpoint := base + "/api/queues/%2F/" + url.PathEscape(opt.queue)
	request, err := http.NewRequest(http.MethodGet, endpoint, nil)
	if err != nil {
		return rabbitSnapshot{}, err
	}
	request.SetBasicAuth(opt.rabbitUser, opt.rabbitPass)
	response, err := client.Do(request)
	if err != nil {
		return rabbitSnapshot{}, err
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusOK {
		return rabbitSnapshot{}, fmt.Errorf("management API returned %s", response.Status)
	}
	var queue queueResponse
	if err := json.NewDecoder(response.Body).Decode(&queue); err != nil {
		return rabbitSnapshot{}, err
	}
	return rabbitSnapshot{
		At: time.Now(), Ready: queue.MessagesReady, Unacknowledged: queue.MessagesUnacknowledged,
		PublishRatePerSecond: queue.MessageStats.Publish.Rate,
		DeliverRatePerSecond: queue.MessageStats.Deliver.Rate,
	}, nil
}

func (c *counters) recordLatency(value time.Duration) {
	n := value.Nanoseconds()
	if n < 1 {
		n = 1
	}
	i := int(math.Log(float64(n)) / math.Log(1.01))
	if i >= len(c.latency.Buckets) {
		i = len(c.latency.Buckets) - 1
	}
	c.mu.Lock()
	c.latency.Buckets[i]++
	c.latency.Samples++
	c.latency.Sum += time.Duration(n)
	if c.latency.MinNanos == 0 || n < c.latency.MinNanos {
		c.latency.MinNanos = n
	}
	if n > c.latency.MaxNanos {
		c.latency.MaxNanos = n
	}
	c.mu.Unlock()
}

func (c *counters) recordStatus(code int) {
	c.mu.Lock()
	c.statuses[code]++
	c.mu.Unlock()
}

func (c *counters) statusCounts() map[int]int64 {
	c.mu.Lock()
	defer c.mu.Unlock()
	copy := make(map[int]int64, len(c.statuses))
	for code, count := range c.statuses {
		copy[code] = count
	}
	return copy
}

func (c *counters) latencySummary() latencySummary {
	c.mu.Lock()
	defer c.mu.Unlock()
	value := c.latency
	if value.Samples == 0 {
		return value
	}
	value.Average = time.Duration(int64(value.Sum) / value.Samples).String()
	value.Min = time.Duration(value.MinNanos).String()
	value.Max = time.Duration(value.MaxNanos).String()
	value.P50 = percentile(value.Buckets[:], value.Samples, 0.50)
	value.P95 = percentile(value.Buckets[:], value.Samples, 0.95)
	value.P99 = percentile(value.Buckets[:], value.Samples, 0.99)
	return value
}

func percentile(buckets []uint64, samples int64, fraction float64) string {
	wanted := uint64(math.Ceil(float64(samples) * fraction))
	var seen uint64
	for i, count := range buckets {
		seen += count
		if seen >= wanted {
			return time.Duration(math.Pow(1.01, float64(i+1))).String()
		}
	}
	return "n/a"
}

func printResult(value result) {
	fmt.Printf("\nResults\n")
	fmt.Printf("  Scheduled requests:     %d\n", value.Scheduled)
	fmt.Printf("  Accepted (202):         %d\n", value.Accepted)
	fmt.Printf("  HTTP failures:           %d\n", value.HTTPFailures)
	fmt.Printf("  Request errors:          %d\n", value.RequestErrors)
	fmt.Printf("  Drops at concurrency cap:%d\n", value.CapacityDrops)
	fmt.Printf("  Accepted rate:           %.1f req/s\n", value.ActualRate)
	fmt.Printf("  Latency p50/p95/p99:     %s / %s / %s (approx.)\n", value.Latency.P50, value.Latency.P95, value.Latency.P99)
	if len(value.RabbitMQ) > 0 {
		first, last := value.RabbitMQ[0], value.RabbitMQ[len(value.RabbitMQ)-1]
		peakReady := first.Ready
		for _, sample := range value.RabbitMQ {
			if sample.Ready > peakReady {
				peakReady = sample.Ready
			}
		}
		fmt.Printf("  Queue ready start/peak/end: %d / %d / %d\n", first.Ready, peakReady, last.Ready)
		fmt.Printf("  Queue unacknowledged:    %d -> %d\n", first.Unacknowledged, last.Unacknowledged)
		fmt.Printf("  Broker publish/deliver:  %.1f / %.1f msg/s (last sample)\n", last.PublishRatePerSecond, last.DeliverRatePerSecond)
		fmt.Printf("  Rabbit sample errors:    %d\n", value.RabbitMQErrors)
	}
}

func writeResult(path string, value result) error {
	file, err := os.Create(path)
	if err != nil {
		return err
	}
	defer file.Close()
	encoder := json.NewEncoder(file)
	encoder.SetIndent("", "  ")
	return encoder.Encode(value)
}
