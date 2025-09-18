package com.techbloghub.output.rss.retry;

import com.techbloghub.domain.crawling.port.FetchRssPort.RssFetchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Component
@Slf4j
public class RetryHandler {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int BASE_DELAY_MS = 1000;

    public <T> T executeWithRetry(String url, Supplier<T> operation) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                if (attempt > 1) {
                    long delay = calculateExponentialBackoffDelay(attempt);
                    log.info("Retrying operation for {} attempt {}/{} after {}ms delay",
                            url, attempt, MAX_RETRY_ATTEMPTS, delay);
                    Thread.sleep(delay);
                }

                return operation.get();

            } catch (HttpClientErrorException | HttpServerErrorException e) {
                lastException = e;

                if (e.getStatusCode().value() == 403 && attempt < MAX_RETRY_ATTEMPTS) {
                    log.warn("Received 403 Forbidden for {}, attempt {}/{}. Will retry.",
                            url, attempt, MAX_RETRY_ATTEMPTS);
                    continue;
                } else {
                    String errorMsg = String.format("HTTP error %d for URL [%s]: %s",
                            e.getStatusCode().value(), url, e.getMessage());
                    log.error(errorMsg, e);
                    throw new RssFetchException(errorMsg, e);
                }

            } catch (ResourceAccessException e) {
                lastException = e;
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.warn("Request failed for {}, attempt {}/{}. Will retry.",
                            url, attempt, MAX_RETRY_ATTEMPTS);
                    continue;
                } else {
                    String errorMsg = String.format("Request failed for URL [%s]: %s", url, e.getMessage());
                    log.error(errorMsg, e);
                    throw new RssFetchException(errorMsg, e);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RssFetchException("Thread interrupted while waiting to retry: " + url, e);

            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.warn("Operation failed for {}, attempt {}/{}. Will retry.",
                            url, attempt, MAX_RETRY_ATTEMPTS);
                    continue;
                } else {
                    String errorMsg = String.format("Failed operation for [%s]: %s", url, e.getMessage());
                    log.error(errorMsg, e);
                    throw new RssFetchException(errorMsg, e);
                }
            }
        }

        // 모든 재시도가 실패한 경우
        String errorMsg = String.format("Failed operation for [%s] after %d attempts", url, MAX_RETRY_ATTEMPTS);
        log.error(errorMsg, lastException);
        throw new RssFetchException(errorMsg, lastException);
    }

    private long calculateExponentialBackoffDelay(int attempt) {
        long baseDelay = BASE_DELAY_MS * (long) Math.pow(2, attempt - 1);
        long jitter = ThreadLocalRandom.current().nextLong(0, baseDelay / 2);
        return baseDelay + jitter;
    }
}