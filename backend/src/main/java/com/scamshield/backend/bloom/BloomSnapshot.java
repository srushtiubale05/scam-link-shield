package com.scamshield.backend.bloom;

import java.time.Instant;

public record BloomSnapshot(String version, int m, int k, int count,
                            Instant builtAt, String data) {}