package com.scamshield.backend.campaign;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityExtractorTest {

    @Test
    void usesRegisteredDomainIncludingMultiPartTlds() {
        assertTrue(EntityExtractor.extract("login.evil-bank.co.in/a").keys().contains("domain:evil-bank.co.in"));
        assertTrue(EntityExtractor.extract("x.y.example.com/a").keys().contains("domain:example.com"));
    }

    @Test
    void sharedHostsUseTheFullHostNotTheRegisteredDomain() {
        var keys = EntityExtractor.extract("ptshopee-69.blogspot.com/a").keys();
        assertTrue(keys.contains("host:ptshopee-69.blogspot.com"));
        assertTrue(keys.stream().noneMatch(k -> k.startsWith("domain:")));
    }

    @Test
    void ipAddressIsItsOwnDomain() {
        assertTrue(EntityExtractor.extract("192.168.1.5/login").keys().contains("domain:192.168.1.5"));
    }

    @Test
    void kitSignatureNeedsThreeSegments() {
        assertTrue(EntityExtractor.extract("a.com/wp-content/plugins/kit/login.php").keys()
                .contains("kit:/wp-content/plugins/kit/login.php"));
        assertTrue(EntityExtractor.extract("a.com/login").keys().stream().noneMatch(k -> k.startsWith("kit:")));
    }

    @Test
    void variableSegmentsAreGeneralized() {
        assertTrue(EntityExtractor.extract("a.com/app/12345/ab12cd34ef/login.php").keys()
                .contains("kit:/app/*/*/login.php"));
        assertTrue(EntityExtractor.extract("a.com/1/2/3").keys().stream().noneMatch(k -> k.startsWith("kit:")));
    }

    @Test
    void queryStringIsIgnoredAndBrandIsDetected() {
        assertTrue(EntityExtractor.extract("a.com/x/y/z.php?id=1").keys().contains("kit:/x/y/z.php"));
        assertEquals("paytm", EntityExtractor.extract("paytm-reward.example.com/x").brand());
        assertNull(EntityExtractor.extract("unrelated.org/a").brand());
    }
}