package com.desensitizer.builtin.desensitizer;

import com.desensitizer.core.api.Desensitizer;

public class IdCardDesensitizer implements Desensitizer {

    @Override
    public String desensitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        int length = value.length();
        if (length == 18) {
            return value.substring(0, 6) + "********" + value.substring(14);
        }

        if (length == 15) {
            return value.substring(0, 6) + "******" + value.substring(12);
        }

        if (length > 15) {
            return value.substring(0, 6) + "********" + value.substring(length - 4);
        }

        return value;
    }
}
