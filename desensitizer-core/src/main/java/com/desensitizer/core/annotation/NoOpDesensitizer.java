package com.desensitizer.core.annotation;

import com.desensitizer.core.api.Desensitizer;

public class NoOpDesensitizer implements Desensitizer {
    @Override
    public String desensitize(String value) {
        return value;
    }
}
