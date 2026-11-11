package com.scamshield.backend.report;

import com.scamshield.backend.user.Role;
import com.scamshield.backend.user.User;
import com.scamshield.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    private ReportRepository reports;
    private ConfirmationRepository confirmations;
    private UserRepository users;
    private ReportService service;

    @BeforeEach
    void setUp() {
        reports = mock(ReportRepository.class);
        confirmations = mock(ConfirmationRepository.class);
        users = mock(UserRepository.class);
        service = new ReportService(reports, confirmations, users, 3);

        when(reports.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));
        when(confirmations.existsByReportIdAndUserId(anyLong(), anyLong())).thenReturn(false);
        when(users.findByEmail("u3@test.com")).thenReturn(Optional.of(user(3L, "u3@test.com")));
    }

    private User user(Long id, String email) {
        return User.builder().id(id).name("U" + id).email(email)
                .passwordHash("x").role(Role.USER).build();
    }

    private Report report(Long id, ReportStatus status, int count) {
        return Report.builder().id(id).originalUrl("x.com/a").normalizedUrl("x.com/a")
                .domain("x.com").category(ReportCategory.PHISHING)
                .status(status).confirmationCount(count)
                .reporter(user(1L, "u1@test.com")).build();
    }

    @Test
    void staysPendingBelowThreshold() {
        when(reports.findById(1L)).thenReturn(Optional.of(report(1L, ReportStatus.PENDING, 1)));
        ReportResponse r = service.confirm("u3@test.com", 1L);
        assertEquals(2, r.confirmationCount());
        assertEquals(ReportStatus.PENDING, r.status());
    }

    @Test
    void becomesVerifiedAtThreshold() {
        when(reports.findById(1L)).thenReturn(Optional.of(report(1L, ReportStatus.PENDING, 2)));
        ReportResponse r = service.confirm("u3@test.com", 1L);
        assertEquals(3, r.confirmationCount());
        assertEquals(ReportStatus.VERIFIED, r.status());
    }

    @Test
    void rejectedReportDoesNotBecomeVerified() {
        when(reports.findById(1L)).thenReturn(Optional.of(report(1L, ReportStatus.REJECTED, 5)));
        ReportResponse r = service.confirm("u3@test.com", 1L);
        assertEquals(ReportStatus.REJECTED, r.status());
    }

    @Test
    void duplicateConfirmationIsRejectedWith409() {
        when(reports.findById(1L)).thenReturn(Optional.of(report(1L, ReportStatus.PENDING, 1)));
        when(confirmations.existsByReportIdAndUserId(1L, 3L)).thenReturn(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.confirm("u3@test.com", 1L));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void newUrlCreatesPendingReportWithOneConfirmation() {
        when(reports.findByNormalizedUrl("fake.xyz/login")).thenReturn(Optional.empty());
        ReportResponse r = service.submit("u3@test.com",
                new ReportRequest("https://WWW.fake.xyz/login?utm_source=wa", ReportCategory.PHISHING));
        assertEquals("fake.xyz/login", r.normalizedUrl());
        assertEquals(1, r.confirmationCount());
        assertEquals(ReportStatus.PENDING, r.status());
    }

    @Test
    void badUrlGives400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.submit("u3@test.com", new ReportRequest("ftp://x.com", ReportCategory.OTHER)));
        assertEquals(400, ex.getStatusCode().value());
    }
}