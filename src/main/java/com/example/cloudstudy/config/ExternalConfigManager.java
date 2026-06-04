package com.example.cloudstudy.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ExternalConfigManager {
    public static final String CONFIG_FILE_NAME = "application.properties";
    private static final String ENV_CONFIG_DIR = "APP_CONFIG_DIR";
    private static final String SYS_PROP_CONFIG_DIR = "app.config.dir";
    private static final String DEFAULT_CONFIG_DIR = ".cloud-study-app/config";

    public Path getConfigDirectory() {
        String envValue = trimToNull(System.getenv(ENV_CONFIG_DIR));
        if (envValue != null) {
            return Paths.get(envValue);
        }

        String propertyValue = trimToNull(System.getProperty(SYS_PROP_CONFIG_DIR));
        if (propertyValue != null) {
            return Paths.get(propertyValue);
        }

        return Paths.get(System.getProperty("user.home"), DEFAULT_CONFIG_DIR);
    }

    public Path getPropertiesFile() {
        return getConfigDirectory().resolve(CONFIG_FILE_NAME);
    }

    public Properties load() throws ConfigStorageException {
        Path file = getPropertiesFile();
        Properties properties = new Properties();

        if (!Files.exists(file)) {
            return properties;
        }

        try (InputStream inputStream = Files.newInputStream(file)) {
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            throw new ConfigStorageException("설정 파일을 읽을 수 없습니다. WAS 실행 사용자에게 파일 읽기 권한이 있는지 확인하세요: "
                    + file.toAbsolutePath(), e);
        }
    }

    public void save(Properties properties) throws ConfigStorageException {
        Path directory = getConfigDirectory();
        Path file = getPropertiesFile();

        try {
            Files.createDirectories(directory);
        } catch (IOException | SecurityException e) {
            throw new ConfigStorageException("설정 디렉터리를 생성할 수 없습니다. APP_CONFIG_DIR 또는 app.config.dir 경로와 권한을 확인하세요: "
                    + directory.toAbsolutePath(), e);
        }

        try (OutputStream outputStream = Files.newOutputStream(file)) {
            properties.store(outputStream, "Cloud Study App external configuration");
        } catch (IOException | SecurityException e) {
            throw new ConfigStorageException("설정 파일을 저장할 수 없습니다. WAS 실행 사용자에게 디렉터리 쓰기 권한이 있는지 확인하세요: "
                    + file.toAbsolutePath(), e);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
