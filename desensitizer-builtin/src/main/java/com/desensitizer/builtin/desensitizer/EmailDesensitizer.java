package com.desensitizer.builtin.desensitizer;

import com.desensitizer.core.api.Desensitizer;

public class EmailDesensitizer implements Desensitizer {

    @Override
    public String desensitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        int atIndex = value.indexOf('@');
        if (atIndex <= 0) {
            return value;
        }

        String username = value.substring(0, atIndex);
        String domain = value.substring(atIndex);

        if (username.length() <= 2) {
            return "**" + domain;
        }

        return username.charAt(0) + "***" + domain;
    }
}
