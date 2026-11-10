package com.scamshield.backend.url;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlNormalizerTest {

    private String n(String url) {
        return UrlNormalizer.normalize(url).normalizedUrl();
    }

    @Test
    void lowercasesHostStripsWwwTrackingAndFragment() {
        assertEquals("scam-site.xyz/Login",
                n("HTTPS://WWW.Scam-Site.XYZ/Login/?utm_source=wa&fbclid=abc#top"));
    }

    @Test
    void addsSchemeWhenMissing() {
        assertEquals("scam-site.xyz/login", n("scam-site.xyz/login"));
    }

    @Test
    void httpAndHttpsMatch() {
        assertEquals(n("http://scam-site.xyz/a"), n("https://scam-site.xyz/a"));
    }

    @Test
    void removesDefaultPortKeepsCustomPort() {
        assertEquals("scam-site.xyz/a", n("http://scam-site.xyz:80/a"));
        assertEquals("scam-site.xyz:8443/a", n("https://scam-site.xyz:8443/a"));
    }

    @Test
    void sortsQueryParams() {
        assertEquals("x.com/p?a=1&b=2", n("http://x.com/p?b=2&a=1"));
    }

    @Test
    void rootAndTrailingSlash() {
        assertEquals("x.com", n("http://x.com/"));
        assertEquals("x.com/a/b", n("http://x.com//a//b/"));
    }

    @Test
    void dropsCredentials() {
        assertEquals("x.com/a", n("http://user:pw@x.com/a"));
    }

    @Test
    void extractsDomain() {
        assertEquals("scam-site.xyz",
                UrlNormalizer.normalize("https://www.scam-site.xyz/path").domain());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> n(""));
        assertThrows(IllegalArgumentException.class, () -> n("   "));
        assertThrows(IllegalArgumentException.class, () -> n(null));
        assertThrows(IllegalArgumentException.class, () -> n("ftp://x.com/file"));
        assertThrows(IllegalArgumentException.class, () -> n("not a url"));
    }
}