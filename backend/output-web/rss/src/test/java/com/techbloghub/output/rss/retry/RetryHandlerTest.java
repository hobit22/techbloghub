package com.techbloghub.output.rss.retry;

import com.techbloghub.domain.port.out.FetchRssPort.RssFetchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryHandlerTest {

    private RetryHandler retryHandler;

    @BeforeEach
    void setUp() {
        retryHandler = new RetryHandler();
    }

    @Test
    void executeWithRetry_successOnFirstAttempt_shouldReturnResult() {
        // given
        String url = "https://example.com/rss/";
        String expectedResult = "success";

        // when
        String result = retryHandler.executeWithRetry(url, () -> expectedResult);

        // then
        assertEquals(expectedResult, result);
    }

    @Test
    void executeWithRetry_successOnSecondAttempt_shouldReturnResult() {
        // given
        String url = "https://example.com/rss/";
        String expectedResult = "success";
        AtomicInteger attemptCount = new AtomicInteger(0);

        // when
        String result = retryHandler.executeWithRetry(url, () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt == 1) {
                throw new ResourceAccessException("Network error");
            }
            return expectedResult;
        });

        // then
        assertEquals(expectedResult, result);
        assertEquals(2, attemptCount.get());
    }

    @Test
    void executeWithRetry_403Error_shouldRetryAndEventuallySucceed() {
        // given
        String url = "https://example.com/rss/";
        String expectedResult = "success";
        AtomicInteger attemptCount = new AtomicInteger(0);

        // when
        String result = retryHandler.executeWithRetry(url, () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt <= 2) {
                throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
            }
            return expectedResult;
        });

        // then
        assertEquals(expectedResult, result);
        assertEquals(3, attemptCount.get());
    }

    @Test
    void executeWithRetry_non403HttpClientError_shouldThrowImmediately() {
        // given
        String url = "https://example.com/rss/";

        // when & then
        RssFetchException exception = assertThrows(RssFetchException.class, () ->
                retryHandler.executeWithRetry(url, () -> {
                    throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
                })
        );

        assertTrue(exception.getMessage().contains("HTTP error 404"));
        assertTrue(exception.getMessage().contains(url));
    }

    @Test
    void executeWithRetry_maxRetriesExceeded_shouldThrowException() {
        // given
        String url = "https://example.com/rss/";
        AtomicInteger attemptCount = new AtomicInteger(0);

        // when & then
        RssFetchException exception = assertThrows(RssFetchException.class, () ->
                retryHandler.executeWithRetry(url, () -> {
                    attemptCount.incrementAndGet();
                    throw new ResourceAccessException("Network error");
                })
        );

        assertEquals(3, attemptCount.get());
        assertTrue(exception.getMessage().contains("Request failed for URL [" + url + "]"));
    }

    @Test
    void executeWithRetry_serverError_shouldRetryAndFail() {
        // given
        String url = "https://example.com/rss/";

        // when & then
        RssFetchException exception = assertThrows(RssFetchException.class, () ->
                retryHandler.executeWithRetry(url, () -> {
                    throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
                })
        );

        assertTrue(exception.getMessage().contains("HTTP error 500"));
    }

    @Test
    void executeWithRetry_interruptedException_shouldThrowRssFetchException() {
        // given
        String url = "https://example.com/rss/";

        // when & then
        RssFetchException exception = assertThrows(RssFetchException.class, () ->
                retryHandler.executeWithRetry(url, () -> {
                    throw new RuntimeException(new InterruptedException("Interrupted"));
                })
        );

        assertTrue(exception.getMessage().contains("Failed operation for [" + url + "]"));
    }

    @Test
    void executeWithRetry_genericException_shouldRetryAndFail() {
        // given
        String url = "https://example.com/rss/";
        AtomicInteger attemptCount = new AtomicInteger(0);

        // when & then
        RssFetchException exception = assertThrows(RssFetchException.class, () ->
                retryHandler.executeWithRetry(url, () -> {
                    attemptCount.incrementAndGet();
                    throw new RuntimeException("Generic error");
                })
        );

        assertEquals(3, attemptCount.get());
        assertTrue(exception.getMessage().contains("Failed operation for [" + url + "]"));
    }
}