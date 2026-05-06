package com.desensitizer.test;

import com.desensitizer.core.registry.DesensitizerRegistry;

public class DesensitizationRunner {

    private final DesensitizerRegistry registry;

    public DesensitizationRunner(DesensitizerRegistry registry) {
        this.registry = registry;
    }

    public String run(String message) {
        return registry.desensitize(message);
    }
}
