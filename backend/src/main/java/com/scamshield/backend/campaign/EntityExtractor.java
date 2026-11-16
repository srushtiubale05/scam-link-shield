package com.scamshield.backend.campaign;

import java.util.*;
import java.util.regex.Pattern;

/** Pulls linkable "entities" out of a normalized URL (host[:port][/path][?query]). */
public final class EntityExtractor {

    public record Entities(Set<String> keys, String brand) {}

    private static final Set<String> SECOND_LEVEL =
            Set.of("co", "com", "org", "net", "gov", "ac", "edu", "nic", "or", "ne");

    /** Hosts where unrelated people get subdomains. Linking by registered domain here would merge strangers. */
    private static final List<String> SHARED_HOSTS = List.of(
            "blogspot.com", "github.io", "weebly.com", "wixsite.com", "web.app", "firebaseapp.com",
            "pages.dev", "netlify.app", "vercel.app", "herokuapp.com", "workers.dev", "godaddysites.com",
            "sites.google.com", "000webhostapp.com", "glitch.me", "repl.co", "onrender.com",
            "azurewebsites.net", "myshopify.com", "notion.site", "carrd.co", "webflow.io", "r2.dev",
            "ngrok.io", "trycloudflare.com", "s3.amazonaws.com", "storage.googleapis.com");

    private static final List<String> BRANDS = List.of(
            "sbi", "hdfc", "icici", "axis", "kotak", "paytm", "phonepe", "gpay", "google", "amazon",
            "flipkart", "npci", "irctc", "aadhaar", "uidai", "whatsapp", "facebook", "instagram",
            "netflix", "microsoft", "apple", "shopee");

    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");
    private static final Pattern VARIABLE = Pattern.compile("\\d+|[0-9a-f]{8,}");

    private EntityExtractor() {}

    public static Entities extract(String normalized) {
        String rest = normalized == null ? "" : normalized.toLowerCase();
        int q = rest.indexOf('?');
        if (q >= 0) rest = rest.substring(0, q);

        String host = rest;
        String path = "";
        int slash = rest.indexOf('/');
        if (slash >= 0) {
            host = rest.substring(0, slash);
            path = rest.substring(slash);
        }
        int colon = host.indexOf(':');
        if (colon >= 0) host = host.substring(0, colon);

        Set<String> keys = new LinkedHashSet<>();
        if (isShared(host)) keys.add("host:" + host);
        else keys.add("domain:" + registeredDomain(host));

        String kit = kitSignature(path);
        if (kit != null) keys.add("kit:" + kit);

        return new Entities(keys, brand(host));
    }

    static boolean isShared(String host) {
        for (String s : SHARED_HOSTS) {
            if (host.equals(s) || host.endsWith("." + s)) return true;
        }
        return false;
    }

    static String registeredDomain(String host) {
        if (IPV4.matcher(host).matches()) return host;
        String[] l = host.split("\\.");
        int n = l.length;
        if (n <= 2) return host;
        if (SECOND_LEVEL.contains(l[n - 2]) && l[n - 1].length() == 2) {
            return l[n - 3] + "." + l[n - 2] + "." + l[n - 1];     // e.g. evil-bank.co.in
        }
        return l[n - 2] + "." + l[n - 1];
    }

    /**
     * Phishing kits are copied between domains, so the same page path on different hosts is a strong hint.
     * Numeric and hash-like segments are generalized to "*". Needs 3+ segments to avoid generic paths.
     */
    static String kitSignature(String path) {
        List<String> segs = new ArrayList<>();
        for (String s : path.split("/")) if (!s.isBlank()) segs.add(s);
        if (segs.size() < 3) return null;

        List<String> out = new ArrayList<>();
        boolean allVariable = true;
        for (String s : segs) {
            if (VARIABLE.matcher(s).matches() || s.length() >= 20) {
                out.add("*");
            } else {
                out.add(s);
                allVariable = false;
            }
        }
        return allVariable ? null : "/" + String.join("/", out);
    }

    static String brand(String host) {
        for (String b : BRANDS) if (host.contains(b)) return b;
        return null;
    }
}