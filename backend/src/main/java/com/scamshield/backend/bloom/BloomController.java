package com.scamshield.backend.bloom;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/bloom")
public class BloomController {

    private final BloomFilterService service;

    public BloomController(BloomFilterService service) {
        this.service = service;
    }

    /** Full filter: the extension downloads this. */
    @GetMapping
    public BloomSnapshot filter() {
        return service.snapshot();
    }

    /** Tiny endpoint: the extension polls this and downloads the filter only if the version changed. */
    @GetMapping("/version")
    public Map<String, Object> version() {
        BloomSnapshot s = service.snapshot();
        return Map.of("version", s.version(), "count", s.count(), "builtAt", s.builtAt());
    }
}