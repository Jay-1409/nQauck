package main

import (
	"encoding/json"
	"io"
	"net/http"
	"strings"
	"time"
)

type requestPayload struct {
	To           string `json:"to"`
	Subject      string `json:"subject"`
	HTMLTemplate string `json:"htmlTemplate"`
}

type sendResult struct {
	status   int
	accepted bool
	latency  time.Duration
	err      error
}

// adapter is the transport boundary; implementations report whether a send
// was accepted and optionally return a protocol status code.
type adapter interface {
	Send(requestPayload) sendResult
}

type httpAdapter struct {
	client   *http.Client
	endpoint string
}

func (a httpAdapter) Send(payload requestPayload) sendResult {
	data, err := json.Marshal(payload)
	if err != nil {
		return sendResult{err: err}
	}
	request, err := http.NewRequest(http.MethodPost, a.endpoint, strings.NewReader(string(data)))
	if err != nil {
		return sendResult{err: err}
	}
	request.Header.Set("Content-Type", "application/json")
	started := time.Now()
	response, err := a.client.Do(request)
	latency := time.Since(started)
	if err != nil {
		return sendResult{latency: latency, err: err}
	}
	_, _ = io.Copy(io.Discard, response.Body)
	_ = response.Body.Close()
	return sendResult{status: response.StatusCode, accepted: response.StatusCode == http.StatusAccepted, latency: latency}
}
