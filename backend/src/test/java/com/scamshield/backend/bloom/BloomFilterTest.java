package com.scamshield.backend.bloom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BloomFilterTest {

    @Test
    void neverReturnsFalseNegatives() {
        BloomFilter f = BloomFilter.forCapacity(10_000, 0.01);
        for (int i = 0; i < 10_000; i++) f.add("scam-" + i + ".xyz/login");
        for (int i = 0; i < 10_000; i++) {
            assertTrue(f.mightContain("scam-" + i + ".xyz/login"));
        }
    }

    @Test
    void falsePositiveRateIsCloseToTarget() {
        int n = 100_000;
        BloomFilter f = BloomFilter.forCapacity(n, 0.01);
        for (int i = 0; i < n; i++) f.add("scam-" + i + ".xyz/login");

        int trials = 100_000, falsePositives = 0;
        for (int i = 0; i < trials; i++) {
            if (f.mightContain("safe-" + i + ".com/home")) falsePositives++;
        }
        double rate = (double) falsePositives / trials;
        System.out.println("m=" + f.getM() + " k=" + f.getK() + " measured FPR=" + rate);
        assertTrue(rate < 0.02, "false positive rate too high: " + rate);
    }

    @Test
    void sizingMatchesTheory() {
        BloomFilter f = BloomFilter.forCapacity(1_000_000, 0.01);
        assertEquals(9_585_059, f.getM(), 5);   // about 1.2 MB for a million URLs
        assertEquals(7, f.getK());
    }

    @Test
    void exportsBase64() {
        BloomFilter f = new BloomFilter(64, 3);
        f.add("x.com");
        assertFalse(f.toBase64().isEmpty());
    }
}