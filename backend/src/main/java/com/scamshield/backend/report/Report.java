package com.scamshield.backend.report;

import com.scamshield.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_url", nullable = false, length = 2000)
    private String originalUrl;

    // One row per scam link: later reports of the same URL become confirmations
    @Column(name = "normalized_url", nullable = false, unique = true, length = 2000)
    private String normalizedUrl;

    @Column(nullable = false)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(name = "confirmation_count", nullable = false)
    @Builder.Default
    private int confirmationCount = 1;   // the reporter counts as the first

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    @Column(length = 20)
    @Builder.Default
    private String source = "COMMUNITY";     // COMMUNITY, OPENPHISH, ...

    @Column(name = "last_seen")
    private Instant lastSeen;                // when a feed last listed this URL
}