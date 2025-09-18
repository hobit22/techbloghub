package com.techbloghub.output.rss.client;

import com.techbloghub.domain.crawling.port.FetchRssPort.RssFetchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Component
@Slf4j
public class HttpRssClient {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36";

    protected final RestTemplate restTemplate;
    private int timeoutSeconds = 30;

    public HttpRssClient() {
        this.restTemplate = new RestTemplate();
    }

    public void setTimeout(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        log.debug("HTTP client timeout set to {} seconds", timeoutSeconds);
    }

    public InputStream fetchContent(String url, boolean isProxyRequest) {
        HttpHeaders headers = createHttpHeaders(isProxyRequest);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        log.debug("Fetching content from: {} (proxy: {})", url, isProxyRequest);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                byte[].class
        );

        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            throw new RssFetchException("Empty content received from: " + url);
        }

        return new ByteArrayInputStream(body);
    }

    private HttpHeaders createHttpHeaders(boolean isProxyRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        headers.set("Accept", "application/rss+xml, application/xml, text/xml, */*");
        headers.set("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8");
        headers.set("Cache-Control", "no-cache");
        headers.set("Connection", "keep-alive");
        headers.set("Referer", "https://www.google.com/");

        // 프록시 요청이 아닌 경우에만 압축 허용 (프록시에서 이미 압축 해제됨)
        if (!isProxyRequest) {
            headers.set("Accept-Encoding", "gzip, deflate, br");
        }

        return headers;
    }
}