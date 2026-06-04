package com.example.cloudstudy.service;

import com.example.cloudstudy.config.AppConfigKeys;
import com.example.cloudstudy.config.ConfigStorageException;
import com.example.cloudstudy.config.ExternalConfigManager;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigService {
    private final ExternalConfigManager configManager;

    public ConfigService(ExternalConfigManager configManager) {
        this.configManager = configManager;
    }

    public Map<String, String> loadMaskedConfig() throws ConfigStorageException {
        Properties properties = configManager.load();
        Map<String, String> result = new LinkedHashMap<>();

        for (String key : AppConfigKeys.ORDERED_KEYS) {
            String value = properties.getProperty(key, "");
            result.put(key, AppConfigKeys.isSensitive(key) ? mask(value) : value);
        }

        return result;
    }

    public void saveFromRequest(Map<String, String> requestValues) throws ConfigStorageException {
        Properties properties = configManager.load();

        for (String key : AppConfigKeys.ORDERED_KEYS) {
            String value = requestValues.get(key);
            if (value == null) {
                continue;
            }

            // 민감값은 화면에 다시 보여주지 않으므로 빈 입력이면 기존 값을 유지한다.
            if (AppConfigKeys.isSensitive(key) && value.trim().isEmpty()) {
                continue;
            }

            properties.setProperty(key, value.trim());
        }

        configManager.save(properties);
    }

    public Path getPropertiesFilePath() {
        return configManager.getPropertiesFile().toAbsolutePath().normalize();
    }

    private String mask(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        if (value.length() <= 4) {
            return "****";
        }

        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}
