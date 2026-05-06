package com.desensitizer.core.config;

import java.util.ArrayList;
import java.util.List;

public class SensitiveConfig {

    private boolean enabled = true;
    private List<SensitiveRule> rules = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<SensitiveRule> getRules() {
        return rules;
    }

    public void setRules(List<SensitiveRule> rules) {
        this.rules = rules;
    }
}