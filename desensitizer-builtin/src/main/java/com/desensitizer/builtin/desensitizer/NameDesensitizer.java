 package com.desensitizer.builtin.desensitizer;

import com.desensitizer.core.api.Desensitizer;

public class NameDesensitizer implements Desensitizer {

    @Override
    public String desensitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int length = value.length();
        if (length <= 1) {
            return "*";
        } else if (length == 2) {
            return value.charAt(0) + "*";
        } else if (length == 3) {
            return value.charAt(0) + "*" + value.charAt(2);
        } else {
            return value.charAt(0) + "**" + value.charAt(length - 1);
        }
    }
}