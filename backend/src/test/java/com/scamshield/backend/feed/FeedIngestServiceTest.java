package com.scamshield.backend.feed;

import com.scamshield.backend.bloom.BloomFilterService;
import com.scamshield.backend.report.*;
import com.scamshield.backend.user.Role;
import com.scamshield.backend.user.User;
import com.scamshield.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedIngestServiceTest {

    private ReportRepository reports;
    private FeedIngestService service;
    private final List<Report> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        reports = mock(ReportRepository.class);
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        BloomFilterService bloom = mock(BloomFilterService.class);

        User system = User.builder().id(99L).name("Threat feeds").email(FeedIngestService.SYSTEM_EMAIL)
                .passwordHash("x").role(Role.USER).build();
        when(users.findByEmail(FeedIngestService.SYSTEM_EMAIL)).thenReturn(Optional.of(system));
        when(reports.findByNormalizedUrlIn(anyCollection())).thenReturn(List.of());
        when(reports.saveAll(any())).thenAnswer(i -> {
            Iterable<Report> it = i.getArgument(0);
            it.forEach(saved::add);
            return saved;
        });

        service = new FeedIngestService(reports, users, encoder, bloom, true, "http://unused", 20_000, 30);
    }

    @Test
    void createsVerifiedReportsDedupesAndSkipsJunk() {
        var summary = service.ingest("OPENPHISH", List.of(
                "https://WWW.Evil-Bank.xyz/login?utm_source=a",
                "evil-bank.xyz/login/",          // same link after normalizing
                "",
                "# comment",
                "ftp://x.com/file",              // unsupported scheme
                "not a url"));                   // invalid

        assertEquals(1, summary.created());
        assertEquals(2, summary.skipped());
        assertEquals(1, saved.size());

        Report r = saved.get(0);
        assertEquals("evil-bank.xyz/login", r.getNormalizedUrl());
        assertEquals(ReportStatus.VERIFIED, r.getStatus());
        assertEquals("OPENPHISH", r.getSource());
        assertEquals(ReportCategory.PHISHING, r.getCategory());
        assertNotNull(r.getLastSeen());
    }

    @Test
    void feedListingVerifiesAPendingCommunityReport() {
        Report pending = Report.builder().id(1L).originalUrl("evil.xyz/a").normalizedUrl("evil.xyz/a")
                .domain("evil.xyz").category(ReportCategory.OTHER).status(ReportStatus.PENDING).build();
        when(reports.findByNormalizedUrlIn(anyCollection())).thenReturn(List.of(pending));

        var summary = service.ingest("OPENPHISH", List.of("https://evil.xyz/a"));

        assertEquals(1, summary.updated());
        assertEquals(0, summary.created());
        assertEquals(ReportStatus.VERIFIED, pending.getStatus());
        assertEquals("COMMUNITY", pending.getSource());     // source of the original report is kept
    }

    @Test
    void adminRejectionIsRespected() {
        Report rejected = Report.builder().id(2L).originalUrl("ok.com/a").normalizedUrl("ok.com/a")
                .domain("ok.com").category(ReportCategory.OTHER).status(ReportStatus.REJECTED).build();
        when(reports.findByNormalizedUrlIn(anyCollection())).thenReturn(List.of(rejected));

        service.ingest("OPENPHISH", List.of("https://ok.com/a"));

        assertEquals(ReportStatus.REJECTED, rejected.getStatus());
    }
}