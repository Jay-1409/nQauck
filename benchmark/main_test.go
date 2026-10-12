package main

import (
	"testing"
	"time"
)

func TestValidateAcceptsKafkaWithoutRabbitMQSettings(t *testing.T) {
	opt := options{
		endpoint: "http://localhost:17431/api/email/send", provider: "kafka", kafkaAddr: "localhost:9092",
		smtpAddr: "localhost:1025", rate: 1, duration: time.Second, maxInFlight: 1, timeout: time.Second,
	}
	if err := validate(opt); err != nil {
		t.Fatal(err)
	}
}

func TestValidateRejectsRabbitMQSamplingForKafka(t *testing.T) {
	opt := options{
		endpoint: "http://localhost:17431/api/email/send", provider: "kafka", kafkaAddr: "localhost:9092",
		smtpAddr: "localhost:1025", rabbitURL: "http://localhost:15672", rate: 1, duration: time.Second,
		maxInFlight: 1, timeout: time.Second,
	}
	if err := validate(opt); err == nil {
		t.Fatal("expected rabbit-url validation error")
	}
}
