package main

import "time"

// loadMethod controls when work is submitted, independently of its transport.
type loadMethod interface {
	Run(rate int, duration time.Duration, submit func()) time.Duration
}

type fixedRate struct{}

func (fixedRate) Run(rate int, duration time.Duration, submit func()) time.Duration {
	interval := time.Second / time.Duration(rate)
	ticker := time.NewTicker(interval)
	timer := time.NewTimer(duration)
	started := time.Now()
	submit()
loop:
	for {
		select {
		case <-ticker.C:
			if time.Since(started) >= duration {
				break loop
			}
			submit()
		case <-timer.C:
			break loop
		}
	}
	ticker.Stop()
	timer.Stop()
	return time.Since(started)
}
