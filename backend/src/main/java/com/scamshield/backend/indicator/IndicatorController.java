package com.scamshield.backend.indicator;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/indicators")
public class IndicatorController {

    private final IndicatorService service;

    public IndicatorController(IndicatorService service) {
        this.service = service;
    }

    @GetMapping("/check")
    public IndicatorService.Result check(@RequestParam String value) {
        return service.check(value);
    }
}