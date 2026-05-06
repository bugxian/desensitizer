package com.desensitizer.builtin.desensitizer;

import com.desensitizer.core.api.Desensitizer;

public class PhoneDesensitizer implements Desensitizer {

    @Override
    public String desensitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        int length = value.length();
        if (length < 7) {
            return value;
        }

        if (length == 11) {
            return value.substring(0, 3) + "****" + value.substring(7);
        }

        if (length < 11) {
            int keepRight = Math.min(2, length - 5);
            return value.substring(0, 3) + "****" + value.substring(length - keepRight);
        }

        return value;
    }
}
