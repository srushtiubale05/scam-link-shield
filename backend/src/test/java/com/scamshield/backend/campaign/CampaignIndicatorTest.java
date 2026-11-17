package com.scamshield.backend.campaign;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CampaignIndicatorTest {

    private CampaignBuilder.Item item(long id, String url, String upi, String phone) {
        return new CampaignBuilder.Item(id, url, "COMMUNITY", "UPI_FRAUD", upi, phone);
    }

    @Test
    void sameUpiIdLinksScamsOnDifferentDomains() {
        var c = CampaignBuilder.build(List.of(
                item(1, "one-scam.example/pay", "refund@okaxis", null),
                item(2, "two-scam.example/pay", "refund@okaxis", null),
                item(3, "other.example/x", "someone@okaxis", null)), 10);

        assertEquals(1, c.size());
        assertEquals(2, c.get(0).size());
        assertTrue(c.get(0).linkedBy().contains("upi:refund@okaxis"));
    }

    @Test
    void phoneNumbersLinkScamsAndAreMaskedInOutput() {
        var c = CampaignBuilder.build(List.of(
                item(1, "one-scam.example/a", null, "9876543210"),
                item(2, "two-scam.example/b", null, "9876543210")), 10);

        assertEquals(1, c.size());
        assertTrue(c.get(0).linkedBy().contains("phone:98****3210"));
        assertTrue(c.get(0).linkedBy().stream().noneMatch(k -> k.contains("9876543210")));
    }
}