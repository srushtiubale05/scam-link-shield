package com.scamshield.backend.url;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class UrlNormalizer {

    public record Result(String normalizedUrl, String domain) {}

    private static final Set<String> TRACKING_PARAMS =
            Set.of("fbclid", "gclid", "msclkid", "igshid", "yclid", "mc_cid", "mc_eid");

    private UrlNormalizer() {}

    public static Result normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("URL is empty");
        }
        String s = raw.trim();
        if (!s.contains("://")) {
            s = "http://" + s;
        }

        URI uri;
        try {
            uri = new URI(s);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL");
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Only http and https URLs are supported");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL has no host");
        }
        host = IDN.toASCII(host.toLowerCase());
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }

        int port = uri.getPort();
        boolean defaultPort = port == -1
                || (scheme.equals("http") && port == 80)
                || (scheme.equals("https") && port == 443);

        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        path = path.replaceAll("/{2,}", "/");
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        String query = "";
        if (uri.getRawQuery() != null) {
            query = Arrays.stream(uri.getRawQuery().split("&"))
                    .filter(p -> !p.isBlank())
                    .filter(p -> !isTracking(p))
                    .sorted()
                    .collect(Collectors.joining("&"));
        }

        StringBuilder sb = new StringBuilder(host);
        if (!defaultPort) sb.append(':').append(port);
        sb.append(path);
        if (!query.isEmpty()) sb.append('?').append(query);

        return new Result(sb.toString(), host);
    }

    private static boolean isTracking(String param) {
        String name = param.contains("=") ? param.substring(0, param.indexOf('=')) : param;
        name = name.toLowerCase();
        return name.startsWith("utm_") || TRACKING_PARAMS.contains(name);
    }
}