package com.scamshield.backend.feed;

import com.scamshield.backend.bloom.BloomFilterService;
import com.scamshield.backend.report.Report;
import com.scamshield.backend.report.ReportCategory;
import com.scamshield.backend.report.ReportRepository;
import com.scamshield.backend.report.ReportStatus;
import com.scamshield.backend.url.UrlNormalizer;
import com.scamshield.backend.user.Role;
import com.scamshield.backend.user.User;
import com.scamshield.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FeedIngestService {

    public record Summary(int read, int created, int updated, int skipped) {}

    private record Entry(String raw, String domain) {}

    static final String SYSTEM_EMAIL = "feeds@scamshield.local";
    private static final String OPENPHISH = "OPENPHISH";
    private static final int CHUNK = 500;
    private static final Logger log = LoggerFactory.getLogger(FeedIngestService.class);

    private final ReportRepository reports;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final BloomFilterService bloom;
    private final boolean enabled;
    private final String openPhishUrl;
    private final int maxUrls;
    private final int expireDays;

    public FeedIngestService(ReportRepository reports, UserRepository users, PasswordEncoder encoder,
                             BloomFilterService bloom,
                             @Value("${app.feeds.enabled:true}") boolean enabled,
                             @Value("${app.feeds.openphish.url:https://openphish.com/feed.txt}") String openPhishUrl,
                             @Value("${app.feeds.max-urls:20000}") int maxUrls,
                             @Value("${app.feeds.expire-days:30}") int expireDays) {
        this.reports = reports;
        this.users = users;
        this.encoder = encoder;
        this.bloom = bloom;
        this.enabled = enabled;
        this.openPhishUrl = openPhishUrl;
        this.maxUrls = maxUrls;
        this.expireDays = expireDays;
    }

    @Scheduled(initialDelayString = "${app.feeds.initial-delay-ms:30000}",
               fixedDelayString = "${app.feeds.interval-ms:21600000}")
    public void runOpenPhish() {
        if (!enabled) return;
        try {
            List<String> lines = download(openPhishUrl);
            Summary s = ingest(OPENPHISH, lines);
            log.info("OpenPhish ingest: {}", s);

            // Only expire old entries after a successful download, so a feed outage never wipes the list
            if (s.read() > 0) {
                int removed = reports.deleteExpiredFeedReports(Instant.now().minus(expireDays, ChronoUnit.DAYS));
                log.info("Expired {} feed entries not seen for {} days", removed, expireDays);
            }
            bloom.rebuild();
        } catch (Exception e) {
            log.warn("OpenPhish ingest failed: {}", e.getMessage());
        }
    }

    /** Normalizes the lines, creates verified reports for new URLs and refreshes known ones. */
    public Summary ingest(String source, List<String> lines) {
        User system = systemUser();
        Instant now = Instant.now();

        Map<String, Entry> unique = new LinkedHashMap<>();
        int read = 0, skipped = 0;
        for (String line : lines) {
            String raw = line.trim();
            if (raw.isEmpty() || raw.startsWith("#")) continue;
            if (unique.size() >= maxUrls) break;
            read++;
            try {
                UrlNormalizer.Result n = UrlNormalizer.normalize(raw);
                if (raw.length() > 2000 || n.normalizedUrl().length() > 2000 || n.domain().length() > 255) {
                    skipped++;
                    continue;
                }
                unique.putIfAbsent(n.normalizedUrl(), new Entry(raw, n.domain()));
            } catch (IllegalArgumentException e) {
                skipped++;
            }
        }

        int created = 0, updated = 0;
        List<String> keys = new ArrayList<>(unique.keySet());
        for (int i = 0; i < keys.size(); i += CHUNK) {
            List<String> chunk = keys.subList(i, Math.min(i + CHUNK, keys.size()));
            Map<String, Report> existing = reports.findByNormalizedUrlIn(chunk).stream()
                    .collect(Collectors.toMap(Report::getNormalizedUrl, r -> r));

            List<Report> toSave = new ArrayList<>();
            for (String key : chunk) {
                Report r = existing.get(key);
                if (r == null) {
                    Entry e = unique.get(key);
                    toSave.add(Report.builder()
                            .originalUrl(e.raw()).normalizedUrl(key).domain(e.domain())
                            .category(ReportCategory.PHISHING)
                            .status(ReportStatus.VERIFIED)
                            .reporter(system)
                            .confirmationCount(0)
                            .source(source)
                            .lastSeen(now).verifiedAt(now)
                            .build());
                    created++;
                } else {
                    r.setLastSeen(now);
                    // a feed listing promotes a pending community report; an admin rejection is respected
                    if (r.getStatus() == ReportStatus.PENDING) {
                        r.setStatus(ReportStatus.VERIFIED);
                        r.setVerifiedAt(now);
                    }
                    toSave.add(r);
                    updated++;
                }
            }
            reports.saveAll(toSave);
        }
        return new Summary(read, created, updated, skipped);
    }

    private User systemUser() {
        return users.findByEmail(SYSTEM_EMAIL).orElseGet(() -> users.save(User.builder()
                .name("Threat feeds").email(SYSTEM_EMAIL)
                .passwordHash(encoder.encode(UUID.randomUUID().toString()))   // nobody can log in as it
                .role(Role.USER).build()));
    }

    private List<String> download(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "ScamLinkShield/1.0 (student project)")
                .GET().build();
        HttpResponse<Stream<String>> res = client.send(req, HttpResponse.BodyHandlers.ofLines());
        if (res.statusCode() != 200) {
            res.body().close();
            throw new IllegalStateException("Feed returned HTTP " + res.statusCode());
        }
        try (Stream<String> body = res.body()) {
            return body.limit(maxUrls + 1000L).toList();
        }
    }
}