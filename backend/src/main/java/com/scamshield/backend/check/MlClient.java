package com.scamshield.backend.check;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

@Component
public class MlClient {

    public record Score(int score, String level) {}

    private static final Logger log = LoggerFactory.getLogger(MlClient.class);
    private final RestClient client;

    public MlClient(@Value("${app.ml.url}") String baseUrl,
                    @Value("${app.ml.timeout-ms}") int timeoutMs) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** Returns empty if the ML service is down or slow, so the check never fails because of it. */
    public Optional<Score> score(String normalizedUrl) {
        try {
            Score s = client.post().uri("/score")
                    .body(Map.of("url", normalizedUrl))
                    .retrieve()
                    .body(Score.class);
            return Optional.ofNullable(s);
        } catch (Exception e) {
            log.warn("ML service unavailable: {}", e.getMessage());
            return Optional.empty();
        }
    }
}