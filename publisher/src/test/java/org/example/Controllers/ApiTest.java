package org.example.Controllers;

import org.example.Entities.EmailRequest;
import org.example.QueueAdaptor.QueueUnavailableException;
import org.example.Services.MessagePublisherService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class ApiTest {

    @Test
    void returnsServiceUnavailableWhenQueueIsOffline() {
        MessagePublisherService publisher = mock(MessagePublisherService.class);
        doThrow(new QueueUnavailableException("Kafka is unavailable", new RuntimeException()))
                .when(publisher).publish(any(), any());
        Api api = new Api(publisher, "email.send", "email.priority.high", "email.priority.medium", "email.priority.low");

        var response = api.sendEmail(new EmailRequest("person@example.com", "Subject", "<p>Body</p>", null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("The message queue is unavailable. Please try again later.",
                ((ProblemDetail) response.getBody()).getDetail());
    }
}
