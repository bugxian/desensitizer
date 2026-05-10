package com.desensitizer.core.config;

import com.desensitizer.core.api.SensitiveType;

import java.util.List;

public class SensitiveRule {

    private SensitiveType type;
    private List<String> fieldNames;

    public SensitiveType getType() {
        return type;
    }

    public void setType(SensitiveType type) {
        this.type = type;
    }

    public List<String> getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(List<String> fieldNames) {
        this.fieldNames = fieldNames;
    }
}