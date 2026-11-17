package com.scamshield.backend.indicator;

import java.util.regex.Pattern;

public final class IndicatorNormalizer {

    private static final Pattern UPI = Pattern.compile("^[a-z0-9][a-z0-9._-]{1,63}@[a-z][a-z0-9]{1,31}$");
    private static final Pattern MOBILE = Pattern.compile("^[6-9]\\d{9}$");

    private IndicatorNormalizer() {}

    /** Returns the normalized UPI ID, null if blank, or throws if it doesn't look like one. */
    public static String upi(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim().toLowerCase();
        if (!UPI.matcher(s).matches()) throw new IllegalArgumentException("Invalid UPI ID (expected name@bank)");
        return s;
    }

    /** Returns a 10-digit Indian mobile number, null if blank, or throws. */
    public static String phone(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String d = raw.replaceAll("\\D", "");
        if (d.length() == 12 && d.startsWith("91")) d = d.substring(2);
        else if (d.length() == 11 && d.startsWith("0")) d = d.substring(1);
        if (!MOBILE.matcher(d).matches()) throw new IllegalArgumentException("Invalid Indian mobile number");
        return d;
    }

    public static String maskPhone(String phone) {
        return phone.length() == 10 ? phone.substring(0, 2) + "****" + phone.substring(6) : "****";
    }
}