package com.scamshield.backend.campaign;

import com.scamshield.backend.report.ReportRepository;
import com.scamshield.backend.report.ReportStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CampaignService {

    private static final long TTL_MS = 5 * 60_000L;
    private static final int KEEP = 50;

    private final ReportRepository reports;
    private volatile List<CampaignBuilder.Campaign> cache = List.of();
    private volatile long cachedAt = 0;

    public CampaignService(ReportRepository reports) {
        this.reports = reports;
    }

    public List<CampaignBuilder.Campaign> top(int limit) {
        if (System.currentTimeMillis() - cachedAt > TTL_MS) {
            List<CampaignBuilder.Item> items = reports.findByStatus(ReportStatus.VERIFIED).stream()
                    .map(r -> new CampaignBuilder.Item(r.getId(), r.getNormalizedUrl(),
                            r.getSource() == null ? "COMMUNITY" : r.getSource(), r.getCategory().name(),
                            r.getUpiId(), r.getPhone()))
                    .toList();
            cache = CampaignBuilder.build(items, KEEP);
            cachedAt = System.currentTimeMillis();
        }
        List<CampaignBuilder.Campaign> c = cache;
        return c.size() > limit ? c.subList(0, limit) : c;
    }
}