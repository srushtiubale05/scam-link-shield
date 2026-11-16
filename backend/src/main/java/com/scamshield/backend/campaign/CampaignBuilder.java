package com.scamshield.backend.campaign;

import java.util.*;

/** Groups scams that share an entity (union-find / connected components). Pure logic, easy to test. */
public final class CampaignBuilder {

    public record Item(long id, String normalizedUrl, String source, String category) {}

    public record Campaign(long id, int size, int domains, String brand, String category,
                           List<String> linkedBy, List<String> sources, List<String> samples) {}

    private CampaignBuilder() {}

    public static List<Campaign> build(List<Item> items, int limit) {
        int n = items.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;

        List<EntityExtractor.Entities> ents = new ArrayList<>(n);
        Map<String, Integer> first = new HashMap<>();
        Map<String, Integer> keyCount = new HashMap<>();

        for (int i = 0; i < n; i++) {
            EntityExtractor.Entities e = EntityExtractor.extract(items.get(i).normalizedUrl());
            ents.add(e);
            for (String key : e.keys()) {
                keyCount.merge(key, 1, Integer::sum);
                Integer prev = first.putIfAbsent(key, i);
                if (prev != null) union(parent, prev, i);
            }
        }

        Map<Integer, List<Integer>> groups = new HashMap<>();
        for (int i = 0; i < n; i++) {
            groups.computeIfAbsent(find(parent, i), k -> new ArrayList<>()).add(i);
        }

        List<Campaign> out = new ArrayList<>();
        for (List<Integer> g : groups.values()) {
            if (g.size() < 2) continue;

            long minId = Long.MAX_VALUE;
            Set<String> domainKeys = new HashSet<>();
            Map<String, Integer> links = new HashMap<>();
            Map<String, Integer> brands = new HashMap<>();
            Map<String, Integer> cats = new HashMap<>();
            Set<String> sources = new TreeSet<>();
            List<String> samples = new ArrayList<>();

            for (int idx : g) {
                Item it = items.get(idx);
                EntityExtractor.Entities e = ents.get(idx);
                minId = Math.min(minId, it.id());
                sources.add(it.source());
                cats.merge(it.category(), 1, Integer::sum);
                if (e.brand() != null) brands.merge(e.brand(), 1, Integer::sum);
                for (String key : e.keys()) {
                    if (key.startsWith("domain:") || key.startsWith("host:")) domainKeys.add(key);
                    if (keyCount.get(key) >= 2) links.merge(key, 1, Integer::sum);   // shared by 2+ scams
                }
                if (samples.size() < 5) samples.add(it.normalizedUrl());
            }

            List<String> linkedBy = links.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .limit(5).map(Map.Entry::getKey).toList();

            out.add(new Campaign(minId, g.size(), domainKeys.size(), top(brands), top(cats),
                    linkedBy, new ArrayList<>(sources), samples));
        }

        out.sort(Comparator.comparingInt(Campaign::size).reversed().thenComparingLong(Campaign::id));
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }

    private static String top(Map<String, Integer> counts) {
        return counts.entrySet().stream()
                .max(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey).orElse(null);
    }

    private static int find(int[] p, int x) {
        while (p[x] != x) {
            p[x] = p[p[x]];     // path compression
            x = p[x];
        }
        return x;
    }

    private static void union(int[] p, int a, int b) {
        int ra = find(p, a), rb = find(p, b);
        if (ra != rb) p[rb] = ra;
    }
}