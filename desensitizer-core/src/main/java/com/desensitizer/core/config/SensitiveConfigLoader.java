package com.desensitizer.core.config;

import com.desensitizer.core.api.SensitiveType;
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

        SensitiveRule phoneRule = new SensitiveRule();
        phoneRule.setType(SensitiveType.PHONE);
        phoneRule.setFieldNames(java.util.Arrays.asList("phone", "phoneNo", "sjh", "dianhua", "shouji"));
        defaultConfig.getRules().add(phoneRule);

        SensitiveRule nameRule = new SensitiveRule();
        nameRule.setType(SensitiveType.NAME);
        nameRule.setFieldNames(java.util.Arrays.asList("name", "xm", "xingming", "xingming"));
        defaultConfig.getRules().add(nameRule);

        SensitiveRule addressRule = new SensitiveRule();
        addressRule.setType(SensitiveType.ADDRESS);
        addressRule.setFieldNames(java.util.Arrays.asList("address", "dizhi", "addr"));
        defaultConfig.getRules().add(addressRule);

        SensitiveRule idCardRule = new SensitiveRule();
        idCardRule.setType(SensitiveType.ID_CARD);
        idCardRule.setFieldNames(java.util.Arrays.asList("idCard", "sfzh", "shenfenzheng"));
        defaultConfig.getRules().add(idCardRule);

        SensitiveRule bankCardRule = new SensitiveRule();
        bankCardRule.setType(SensitiveType.BANK_CARD);
        bankCardRule.setFieldNames(java.util.Arrays.asList("bankCard", "yhkh", "yinhangkahao"));
        defaultConfig.getRules().add(bankCardRule);

        SensitiveRule passwordRule = new SensitiveRule();
        passwordRule.setType(SensitiveType.PASSWORD);
        passwordRule.setFieldNames(java.util.Arrays.asList("password", "pwd", "mima"));
        defaultConfig.getRules().add(passwordRule);

        SensitiveRule emailRule = new SensitiveRule();
        emailRule.setType(SensitiveType.EMAIL);
        emailRule.setFieldNames(java.util.Arrays.asList("email", "dzyj", "youxiang"));
        defaultConfig.getRules().add(emailRule);

        return defaultConfig;
    }

    public static synchronized void reset() {
        config = null;
    }

    public static void setConfig(SensitiveConfig newConfig) {
        config = newConfig;
    }
}