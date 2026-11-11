package com.scamshield.backend.bloom;

import com.scamshield.backend.report.Report;
import com.scamshield.backend.report.ReportRepository;
import com.scamshield.backend.report.ReportStatus;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class BloomFilterService {

    private static final int MIN_CAPACITY = 10_000;
    private static final double FALSE_POSITIVE_RATE = 0.01;

    private final ReportRepository reportRepository;
    private final AtomicReference<BloomSnapshot> current = new AtomicReference<>();

    public BloomFilterService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void buildOnStartup() {
        rebuild();
    }

    @Scheduled(fixedDelayString = "${app.bloom.rebuild-ms:600000}")
    public void rebuild() {
        List<Report> verified = reportRepository.findByStatus(ReportStatus.VERIFIED);

        // Leave room to grow so the real false-positive rate stays near the target
        int capacity = Math.max(MIN_CAPACITY, verified.size() * 2);
        BloomFilter filter = BloomFilter.forCapacity(capacity, FALSE_POSITIVE_RATE);
        for (Report r : verified) {
            filter.add(r.getNormalizedUrl());
        }

        String data = filter.toBase64();
        current.set(new BloomSnapshot(sha256Short(data + filter.getM() + filter.getK()),
                filter.getM(), filter.getK(), verified.size(), Instant.now(), data));
    }

    public BloomSnapshot snapshot() {
        BloomSnapshot s = current.get();
        if (s == null) {          // first request before startup build finished
            rebuild();
            s = current.get();
        }
        return s;
    }

    // Version = hash of the content, so clients only re-download when it really changed
    private static String sha256Short(String text) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d).substring(0, 12);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}