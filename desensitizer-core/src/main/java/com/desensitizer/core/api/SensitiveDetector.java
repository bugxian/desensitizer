package com.desensitizer.core.api;

import java.util.List;

public interface SensitiveDetector {
    List<SensitiveMatch> detect(String text);
    String name();
    default boolean enabled() {
        return true;
    }
}
