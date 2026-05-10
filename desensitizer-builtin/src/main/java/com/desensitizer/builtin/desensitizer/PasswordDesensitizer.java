package com.desensitizer.builtin.desensitizer;

import com.desensitizer.core.api.Desensitizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordDesensitizer implements Desensitizer {

    private static final int MASK_LENGTH = 16;
    private static final String MASK = "****************";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(?:password|pwd|密码)\\s*[:=]\\s*(\\S+)");

    @Override
    public String desensitize(String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }

        Matcher matcher = PASSWORD_PATTERN.matcher(value);
        if (matcher.matches()) {
            String passwordValue = matcher.group(1);
            int separatorIndex = value.indexOf(passwordValue);
            if (separatorIndex > 0) {
                String prefix = value.substring(0, separatorIndex);
                String mask = generateMask(passwordValue.length());
                return prefix + mask;
            }
        }

        return MASK;
    }

    private String generateMask(int length) {
        if (length <= 0) {
            return MASK;
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append("*");
        }
        return sb.toString();
    }
}
