package com.desensitizer.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;

public class SensitiveConfigLoader {

    private static final String DEFAULT_CONFIG_PATH = "sensitive-config.yaml";

    private static volatile SensitiveConfig config;

    public static SensitiveConfig load() {
        if (config != null) {
            return config;
        }
        return load(DEFAULT_CONFIG_PATH);
    }

    public static SensitiveConfig load(String path) {
        if (config != null) {
            return config;
        }
        synchronized (SensitiveConfigLoader.class) {
            if (config != null) {
                return config;
            }
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            try (InputStream is = SensitiveConfigLoader.class.getClassLoader().getResourceAsStream(path)) {
                if (is == null) {
                    config = createDefaultConfig();
                    return config;
                }
                SensitiveConfigWrapper wrapper = mapper.readValue(is, SensitiveConfigWrapper.class);
                if (wrapper == null || wrapper.getSensitive() == null) {
                    config = createDefaultConfig();
                    return config;
                }
                config = wrapper.getSensitive();
                return config;
            } catch (IOException e) {
                config = createDefaultConfig();
                return config;
            }
        }
    }

    private static SensitiveConfig createDefaultConfig() {
        SensitiveConfig defaultConfig = new SensitiveConfig();
        defaultConfig.setEnabled(true);
        return defaultConfig;
    }

    public static synchronized void reset() {
        config = null;
    }

    public static void setConfig(SensitiveConfig newConfig) {
        config = newConfig;
    }
}