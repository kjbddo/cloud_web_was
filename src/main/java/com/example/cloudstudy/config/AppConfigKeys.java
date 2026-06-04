package com.example.cloudstudy.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AppConfigKeys {
    public static final String DB_ENDPOINT = "db.endpoint";
    public static final String DB_USERNAME = "db.username";
    public static final String DB_PASSWORD = "db.password";
    public static final String STORAGE_ACCOUNT = "storage.account";
    public static final String STORAGE_CONTAINER = "storage.container";
    public static final String STORAGE_SAS = "storage.sas";
    public static final String STORAGE_FILE_SHARE = "storage.file.share";
    public static final String STORAGE_FILE_DIRECTORY = "storage.file.directory";
    public static final String WAS_BASE_URL = "was.baseUrl";

    public static final Set<String> ORDERED_KEYS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            DB_ENDPOINT,
            DB_USERNAME,
            DB_PASSWORD,
            STORAGE_ACCOUNT,
            STORAGE_CONTAINER,
            STORAGE_SAS,
            STORAGE_FILE_SHARE,
            STORAGE_FILE_DIRECTORY,
            WAS_BASE_URL
    )));

    private static final Set<String> SENSITIVE_KEYS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            DB_PASSWORD,
            STORAGE_SAS
    )));

    private AppConfigKeys() {
    }

    public static boolean isSensitive(String key) {
        return SENSITIVE_KEYS.contains(key);
    }
}
