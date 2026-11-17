package com.scamshield.backend.indicator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndicatorNormalizerTest {

    @Test
    void normalizesUpiIds() {
        assertEquals("refund.help@okaxis", IndicatorNormalizer.upi("  Refund.Help@OKAxis "));
        assertNull(IndicatorNormalizer.upi(""));
        assertNull(IndicatorNormalizer.upi(null));
    }

    @Test
    void rejectsBadUpiIds() {
        assertThrows(IllegalArgumentException.class, () -> IndicatorNormalizer.upi("not-an-upi"));
        assertThrows(IllegalArgumentException.class, () -> IndicatorNormalizer.upi("a@b"));
        assertThrows(IllegalArgumentException.class, () -> IndicatorNormalizer.upi("x y@okaxis"));
    }

    @Test
    void normalizesIndianMobileNumbers() {
        assertEquals("9876543210", IndicatorNormalizer.phone("+91 98765-43210"));
        assertEquals("9876543210", IndicatorNormalizer.phone("09876543210"));
        assertEquals("9876543210", IndicatorNormalizer.phone("98765 43210"));
        assertNull(IndicatorNormalizer.phone(" "));
    }

    @Test
    void rejectsBadPhoneNumbers() {
        assertThrows(IllegalArgumentException.class, () -> IndicatorNormalizer.phone("12345"));
        assertThrows(IllegalArgumentException.class, () -> IndicatorNormalizer.phone("5876543210"));   // must start 6-9
    }

    @Test
    void masksPhones() {
        assertEquals("98****3210", IndicatorNormalizer.maskPhone("9876543210"));
    }
}