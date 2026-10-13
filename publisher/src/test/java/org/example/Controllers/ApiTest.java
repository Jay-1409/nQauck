package org.example.Controllers;

import org.example.Entities.EmailRequest;
import org.example.QueueAdaptor.QueueUnavailableException;
import org.example.Security.AccessCoinStore;
import org.example.Services.MessagePublisherService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiTest {

    @Test
    void returnsUnauthorizedWhenAccessCoinIsMissing() {
        MessagePublisherService publisher = mock(MessagePublisherService.class);
        AccessCoinStore coinStore = mock(AccessCoinStore.class);
        Api api = new Api(publisher, coinStore, "email.send", "email.priority.high", "email.priority.medium", "email.priority.low");

        var response = api.sendEmail(null, null, new EmailRequest("person@example.com", "Subject", "<p>Body</p>", null));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Missing or invalid AccessCoin. Provide a valid token in X-Access-Coin or Authorization: Bearer header.",
                ((ProblemDetail) response.getBody()).getDetail());
    }

    @Test
    void returnsUnauthorizedWhenAccessCoinIsInvalid() {
        MessagePublisherService publisher = mock(MessagePublisherService.class);
        AccessCoinStore coinStore = mock(AccessCoinStore.class);
        when(coinStore.isValid("invalid_coin")).thenReturn(false);
        Api api = new Api(publisher, coinStore, "email.send", "email.priority.high", "email.priority.medium", "email.priority.low");

        var response = api.sendEmail("invalid_coin", null, new EmailRequest("person@example.com", "Subject", "<p>Body</p>", null));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void acceptsRequestWhenValidAccessCoinHeaderProvided() {
        MessagePublisherService publisher = mock(MessagePublisherService.class);
        AccessCoinStore coinStore = mock(AccessCoinStore.class);
        when(coinStore.isValid("ac_valid_123")).thenReturn(true);
        Api api = new Api(publisher, coinStore, "email.send", "email.priority.high", "email.priority.medium", "email.priority.low");

        var response = api.sendEmail("ac_valid_123", null, new EmailRequest("person@example.com", "Subject", "<p>Body</p>", null));

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    void acceptsRequestWhenValidBearerTokenProvided() {
        MessagePublisherService publisher = mock(MessagePublisherService.class);
        AccessCoinStore coinStore = mock(AccessCoinStore.class);
        when(coinStore.isValid("ac_bearer_456")).thenReturn(true);
        Api api = new Api(publisher, coinStore, "email.send", "email.priority.high", "email.priority.medium", "email.priority.low");

        var response = api.sendEmail(null, "Bearer ac_bearer_456", new EmailRequest("person@example.com", "Subject", "<p>Body</p>", null));

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    void returnsServiceUnavailableWhenQueueIsOffline() {
        MessagePublisherService publisher = mock(MessagePublisherService.class);
        AccessCoinStore coinStore = mock(AccessCoinStore.class);
        when(coinStore.isValid("ac_valid_123")).thenReturn(true);
        doThrow(new QueueUnavailableException("Kafka is unavailable", new RuntimeException()))
                .when(publisher).publish(any(), any());
        Api api = new Api(publisher, coinStore, "email.send", "email.priority.high", "email.priority.medium", "email.priority.low");

        var response = api.sendEmail("ac_valid_123", null, new EmailRequest("person@example.com", "Subject", "<p>Body</p>", null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("The message queue is unavailable. Please try again later.",
                ((ProblemDetail) response.getBody()).getDetail());
    }

    @Test
    void rejectsRequestWithMissingFields() {
        MessagePublisherService publisher = mock(MessagePublisherService.class);
        AccessCoinStore coinStore = mock(AccessCoinStore.class);
        when(coinStore.isValid("ac_valid")).thenReturn(true);
        Api api = new Api(publisher, coinStore, "email.send", "email.priority.high", "email.priority.medium", "email.priority.low");

        var response = api.sendEmail("ac_valid", null, new EmailRequest("", "Subject", "<p>Body</p>", null));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        response = api.sendEmail("ac_valid", null, new EmailRequest("test@example.com", "", "<p>Body</p>", null));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        response = api.sendEmail("ac_valid", null, new EmailRequest("test@example.com", "Subject", "   ", null));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void rejectsRequestWithInvalidEmail() {
        MessagePublisherService publisher = mock(MessagePublisherService.class);
        AccessCoinStore coinStore = mock(AccessCoinStore.class);
        when(coinStore.isValid("ac_valid")).thenReturn(true);
        Api api = new Api(publisher, coinStore, "email.send", "email.priority.high", "email.priority.medium", "email.priority.low");

        var response = api.sendEmail("ac_valid", null, new EmailRequest("not-an-email", "Subject", "<p>Body</p>", null));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Recipient address must be a valid email.", ((ProblemDetail) response.getBody()).getDetail());
    }
}
