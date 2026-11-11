package com.scamshield.backend.bloom;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.function.IntPredicate;

class BloomBenchmark {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));   // same fix as the app
    }

    private static final String JDBC =
            "jdbc:postgresql://localhost:5432/scamshield?reWriteBatchedInserts=true";
    private static final int QUERIES = 500_000;     // half known scams, half unknown URLs
    private static final int DB_SAMPLES = 2_000;

    static volatile long sink;                       // stops the JIT from removing the lookups

    @Test
    void run() throws Exception {
        List<String> csv = new ArrayList<>();
        csv.add("n,bloom_bytes,raw_string_bytes,memory_ratio,bloom_build_ms,"
                + "bloom_ns_per_lookup,hashset_ns_per_lookup,db_ns_per_lookup,"
                + "theoretical_fpr,measured_fpr");

        for (int n : new int[]{100_000, 1_000_000}) {
            csv.add(bench(n));
        }

        Path out = Path.of("..", "benchmark", "results.csv");
        Files.createDirectories(out.getParent());
        Files.write(out, csv);
        System.out.println("\nSaved " + out.toAbsolutePath().normalize());
    }

    private String bench(int n) {
        System.out.println("\n=== n = " + n + " ===");

        String[] urls = new String[n];
        long rawBytes = 0;
        for (int i = 0; i < n; i++) {
            urls[i] = "scam-" + i + "-kyc-verify.xyz/login?id=" + (i * 7919L);
            rawBytes += urls[i].getBytes(StandardCharsets.UTF_8).length;
        }

        // even index = known scam, odd index = URL that was never added
        String[] queries = new String[QUERIES];
        Random rnd = new Random(42);
        for (int i = 0; i < QUERIES; i++) {
            queries[i] = (i % 2 == 0) ? urls[rnd.nextInt(n)] : "safe-" + i + ".com/home";
        }

        long t0 = System.nanoTime();
        BloomFilter bloom = BloomFilter.forCapacity(n, 0.01);
        for (String u : urls) bloom.add(u);
        double buildMs = (System.nanoTime() - t0) / 1e6;

        Set<String> set = new HashSet<>(n * 2);
        Collections.addAll(set, urls);

        double bloomNs = time(QUERIES, i -> bloom.mightContain(queries[i]));
        double setNs = time(QUERIES, i -> set.contains(queries[i]));
        double dbNs = dbLookup(urls, queries);

        int misses = 0, falsePositives = 0;
        for (int i = 1; i < QUERIES; i += 2) {
            misses++;
            if (bloom.mightContain(queries[i])) falsePositives++;
        }
        double measuredFpr = (double) falsePositives / misses;
        double theoreticalFpr =
                Math.pow(1 - Math.exp(-(double) bloom.getK() * n / bloom.getM()), bloom.getK());

        long bloomBytes = (bloom.getM() + 7) / 8;
        double ratio = (double) rawBytes / bloomBytes;

        System.out.printf(Locale.ROOT, "m=%d bits, k=%d%n", bloom.getM(), bloom.getK());
        System.out.printf(Locale.ROOT, "memory   : bloom %.2f MB vs raw strings %.2f MB (%.1fx smaller)%n",
                bloomBytes / 1e6, rawBytes / 1e6, ratio);
        System.out.printf(Locale.ROOT, "lookup   : bloom %.0f ns | hashset %.0f ns | postgres %s%n",
                bloomNs, setNs, Double.isNaN(dbNs) ? "n/a" : String.format(Locale.ROOT, "%.0f ns (%.1f us)", dbNs, dbNs / 1000));
        System.out.printf(Locale.ROOT, "fpr      : theoretical %.4f | measured %.4f%n", theoreticalFpr, measuredFpr);

        return String.format(Locale.ROOT, "%d,%d,%d,%.1f,%.0f,%.0f,%.0f,%s,%.5f,%.5f",
                n, bloomBytes, rawBytes, ratio, buildMs, bloomNs, setNs,
                Double.isNaN(dbNs) ? "n/a" : String.format(Locale.ROOT, "%.0f", dbNs),
                theoreticalFpr, measuredFpr);
    }

    /** Average nanoseconds per call, after a warm-up pass so the JIT has compiled the code. */
    private static double time(int q, IntPredicate lookup) {
        long hits = 0;
        for (int i = 0; i < q; i++) if (lookup.test(i)) hits++;
        long t0 = System.nanoTime();
        for (int i = 0; i < q; i++) if (lookup.test(i)) hits++;
        long elapsed = System.nanoTime() - t0;
        sink += hits;
        return (double) elapsed / q;
    }

    /** Indexed exact-match query against local PostgreSQL. Returns NaN if the DB isn't reachable. */
    private static double dbLookup(String[] urls, String[] queries) {
        try (Connection c = DriverManager.getConnection(JDBC, "scam", "scam123")) {
            try (Statement s = c.createStatement()) {
                s.execute("drop table if exists bench_urls");
                s.execute("create table bench_urls(url text primary key)");   // primary key = B-tree index
            }
            c.setAutoCommit(false);
            try (PreparedStatement ins = c.prepareStatement("insert into bench_urls(url) values (?)")) {
                for (int i = 0; i < urls.length; i++) {
                    ins.setString(1, urls[i]);
                    ins.addBatch();
                    if ((i + 1) % 10_000 == 0) ins.executeBatch();
                }
                ins.executeBatch();
            }
            c.commit();
            c.setAutoCommit(true);
            try (Statement s = c.createStatement()) { s.execute("analyze bench_urls"); }

            long hits = 0;
            try (PreparedStatement sel = c.prepareStatement("select 1 from bench_urls where url = ?")) {
                for (int i = 0; i < 200; i++) hits += one(sel, queries[i]);           // warm-up
                long t0 = System.nanoTime();
                for (int i = 0; i < DB_SAMPLES; i++) hits += one(sel, queries[i]);
                double perLookup = (double) (System.nanoTime() - t0) / DB_SAMPLES;
                sink += hits;
                try (Statement s = c.createStatement()) { s.execute("drop table bench_urls"); }
                return perLookup;
            }
        } catch (SQLException e) {
            System.out.println("DB benchmark skipped: " + e.getMessage());
            return Double.NaN;
        }
    }

    private static int one(PreparedStatement sel, String url) throws SQLException {
        sel.setString(1, url);
        try (ResultSet rs = sel.executeQuery()) {
            return rs.next() ? 1 : 0;
        }
    }
}