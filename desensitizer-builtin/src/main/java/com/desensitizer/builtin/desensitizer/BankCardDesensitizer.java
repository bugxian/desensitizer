package com.desensitizer.builtin.desensitizer;

import com.desensitizer.core.api.Desensitizer;

public class BankCardDesensitizer implements Desensitizer {

    @Override
    public String desensitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        int length = value.length();
        if (length < 16) {
            return value;
        }

        if (length >= 16) {
            return value.substring(0, 6) + "********" + value.substring(length - 4);
        }

        return value;
    }
}
