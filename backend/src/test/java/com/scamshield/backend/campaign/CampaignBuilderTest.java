package com.scamshield.backend.campaign;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CampaignBuilderTest {

    private CampaignBuilder.Item item(long id, String url) {
        return new CampaignBuilder.Item(id, url, "OPENPHISH", "PHISHING");
    }

    @Test
    void groupsByKitAndDomainAndIgnoresUnrelatedScams() {
        var campaigns = CampaignBuilder.build(List.of(
                item(1, "evil-one.com/wp-content/plugins/kit/login.php"),
                item(2, "other-two.net/wp-content/plugins/kit/login.php"),   // same kit as 1
                item(3, "unrelated.org/a"),                                   // alone
                item(4, "a.blogspot.com/x"),                                  // shared host
                item(5, "b.blogspot.com/y"),                                  // shared host, different user
                item(6, "shop.evil-one.com/promo")), 10);                     // same domain as 1

        assertEquals(1, campaigns.size());
        var c = campaigns.get(0);
        assertEquals(3, c.size());
        assertEquals(2, c.domains());
        assertEquals(1L, c.id());
        assertTrue(c.linkedBy().contains("kit:/wp-content/plugins/kit/login.php"));
        assertTrue(c.linkedBy().contains("domain:evil-one.com"));
    }

    @Test
    void sharedHostingDoesNotMergeStrangers() {
        var campaigns = CampaignBuilder.build(List.of(
                item(1, "a.blogspot.com/x"), item(2, "b.blogspot.com/y")), 10);
        assertTrue(campaigns.isEmpty());
    }

    @Test
    void biggestCampaignComesFirstAndLimitApplies() {
        var campaigns = CampaignBuilder.build(List.of(
                item(1, "aa.com/1"), item(2, "x.aa.com/2"),                              // size 2
                item(3, "bb.com/1"), item(4, "x.bb.com/2"), item(5, "y.bb.com/3")), 1);  // size 3
        assertEquals(1, campaigns.size());
        assertEquals(3, campaigns.get(0).size());
    }
}