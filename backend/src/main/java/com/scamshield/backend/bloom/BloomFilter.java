package com.scamshield.backend.bloom;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.BitSet;

public class BloomFilter {

    private final BitSet bits;
    private final int m;   // number of bits
    private final int k;   // number of hash functions

    public BloomFilter(int m, int k) {
        if (m <= 0 || k <= 0) throw new IllegalArgumentException("m and k must be positive");
        this.m = m;
        this.k = k;
        this.bits = new BitSet(m);
    }

    /** Pick m and k from the expected item count n and the target false-positive rate p. */
    public static BloomFilter forCapacity(int n, double p) {
        int m = (int) Math.ceil(-n * Math.log(p) / (Math.log(2) * Math.log(2)));
        int k = Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
        return new BloomFilter(m, k);
    }

    public void add(String item) {
        long[] h = hashes(item);
        for (int i = 0; i < k; i++) {
            bits.set(index(h, i));
        }
    }

    public boolean mightContain(String item) {
        long[] h = hashes(item);
        for (int i = 0; i < k; i++) {
            if (!bits.get(index(h, i))) return false;   // definitely not present
        }
        return true;                                    // probably present
    }

    // Double hashing: g_i(x) = h1(x) + i * h2(x)  (mod m)
    private int index(long[] h, int i) {
        long combined = h[0] + (long) i * h[1];
        return (int) Math.floorMod(combined, (long) m);
    }

    // Two independent 32-bit FNV-1a style hashes (easy to port to JavaScript)
    private static long[] hashes(String s) {
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        long h1 = fnv1a(data, 0x811C9DC5L);
        long h2 = fnv1a(data, 0x01000193L ^ 0x9E3779B9L) | 1;  // force odd so it never collapses to 0
        return new long[]{h1, h2};
    }

    private static long fnv1a(byte[] data, long seed) {
        long hash = seed & 0xFFFFFFFFL;
        for (byte b : data) {
            hash ^= (b & 0xFFL);
            hash = (hash * 0x01000193L) & 0xFFFFFFFFL;
        }
        return hash;
    }

    public int getM() { return m; }
    public int getK() { return k; }
    public String toBase64() { return Base64.getEncoder().encodeToString(bits.toByteArray()); }
}