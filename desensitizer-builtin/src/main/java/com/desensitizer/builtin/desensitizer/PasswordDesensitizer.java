package com.desensitizer.builtin.desensitizer;

import com.desensitizer.core.api.Desensitizer;

public class PasswordDesensitizer implements Desensitizer {

    @Override
    public String desensitize(String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }
        int length = Math.max(7, value.length());
        return "*".repeat(length);
    }
}
