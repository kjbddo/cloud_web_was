package com.example.cloudstudy.service;

import com.example.cloudstudy.config.AppConfigKeys;
import com.example.cloudstudy.config.ConfigStorageException;
import com.example.cloudstudy.config.ExternalConfigManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DatabaseService {
    private final ExternalConfigManager configManager;

    public DatabaseService(ExternalConfigManager configManager) {
        this.configManager = configManager;
    }

    public Map<String, Object> testConnection() throws ConfigStorageException, SQLException {
        try (Connection connection = openConnection()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("connected", true);
            result.put("databaseProduct", connection.getMetaData().getDatabaseProductName());
            result.put("databaseVersion", connection.getMetaData().getDatabaseProductVersion());
            result.put("jdbcUrl", connection.getMetaData().getURL());
            return result;
        }
    }

    public List<Map<String, Object>> listItems() throws ConfigStorageException, SQLException {
        try (Connection connection = openConnection()) {
            ensureTable(connection);

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, asset_name, asset_type, owner_name, status, description, created_at, updated_at "
                            + "FROM cloud_assets ORDER BY id DESC");
                 ResultSet resultSet = statement.executeQuery()) {
                List<Map<String, Object>> items = new ArrayList<>();
                while (resultSet.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", resultSet.getLong("id"));
                    item.put("assetName", resultSet.getString("asset_name"));
                    item.put("assetType", resultSet.getString("asset_type"));
                    item.put("ownerName", resultSet.getString("owner_name"));
                    item.put("status", resultSet.getString("status"));
                    item.put("description", resultSet.getString("description"));
                    item.put("createdAt", String.valueOf(resultSet.getTimestamp("created_at")));
                    item.put("updatedAt", String.valueOf(resultSet.getTimestamp("updated_at")));
                    items.add(item);
                }
                return items;
            }
        }
    }

    public Map<String, Object> createItem(String assetName, String assetType, String ownerName, String status, String description)
            throws ConfigStorageException, SQLException {
        validateText(assetName, "리소스 이름");
        validateText(assetType, "리소스 유형");
        validateText(ownerName, "소유자");
        validateText(status, "상태");

        try (Connection connection = openConnection()) {
            ensureTable(connection);

            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO cloud_assets (asset_name, asset_type, owner_name, status, description) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, assetName);
                statement.setString(2, assetType);
                statement.setString(3, ownerName);
                statement.setString(4, status);
                statement.setString(5, description == null ? "" : description);
                statement.executeUpdate();

                long id = 0L;
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        id = keys.getLong(1);
                    }
                }

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("message", "클라우드 리소스 항목을 추가했습니다.");
                result.put("id", id);
                return result;
            }
        }
    }

    public Map<String, Object> updateItem(long id, String assetName, String assetType, String ownerName, String status,
                                          String description) throws ConfigStorageException, SQLException {
        validateText(assetName, "리소스 이름");
        validateText(assetType, "리소스 유형");
        validateText(ownerName, "소유자");
        validateText(status, "상태");

        try (Connection connection = openConnection()) {
            ensureTable(connection);

            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE cloud_assets SET asset_name = ?, asset_type = ?, owner_name = ?, status = ?, "
                            + "description = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                statement.setString(1, assetName);
                statement.setString(2, assetType);
                statement.setString(3, ownerName);
                statement.setString(4, status);
                statement.setString(5, description == null ? "" : description);
                statement.setLong(6, id);
                int updated = statement.executeUpdate();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("message", updated > 0 ? "클라우드 리소스 항목을 수정했습니다." : "수정할 항목을 찾지 못했습니다.");
                result.put("updated", updated);
                return result;
            }
        }
    }

    public Map<String, Object> deleteItem(long id) throws ConfigStorageException, SQLException {
        try (Connection connection = openConnection()) {
            ensureTable(connection);

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM cloud_assets WHERE id = ?")) {
                statement.setLong(1, id);
                int deleted = statement.executeUpdate();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("message", deleted > 0 ? "클라우드 리소스 항목을 삭제했습니다." : "삭제할 항목을 찾지 못했습니다.");
                result.put("deleted", deleted);
                return result;
            }
        }
    }

    private Connection openConnection() throws ConfigStorageException, SQLException {
        Properties properties = configManager.load();
        String endpoint = trimToNull(properties.getProperty(AppConfigKeys.DB_ENDPOINT));
        String username = trimToNull(properties.getProperty(AppConfigKeys.DB_USERNAME));
        String password = properties.getProperty(AppConfigKeys.DB_PASSWORD, "");

        if (endpoint == null) {
            throw new SQLException("db.endpoint 설정이 비어 있습니다. /config.html에서 JDBC URL을 저장하세요.");
        }

        loadJdbcDriver(endpoint);

        if (username == null) {
            return DriverManager.getConnection(endpoint);
        }

        return DriverManager.getConnection(endpoint, username, password);
    }

    private void loadJdbcDriver(String endpoint) throws SQLException {
        String lowerEndpoint = endpoint.toLowerCase();
        String driverClassName = null;

        if (lowerEndpoint.startsWith("jdbc:mysql:")) {
            driverClassName = "com.mysql.cj.jdbc.Driver";
        } else if (lowerEndpoint.startsWith("jdbc:postgresql:")) {
            driverClassName = "org.postgresql.Driver";
        } else if (lowerEndpoint.startsWith("jdbc:sqlserver:")) {
            driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        }

        if (driverClassName == null) {
            return;
        }

        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC 드라이버 클래스를 찾을 수 없습니다: " + driverClassName
                    + ". WAR에 드라이버가 포함되어 있는지 또는 Tomcat lib에 배치했는지 확인하세요.", e);
        }
    }

    private void ensureTable(Connection connection) throws SQLException {
        String url = connection.getMetaData().getURL().toLowerCase();
        String sql;

        if (url.contains("sqlserver")) {
            sql = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='cloud_assets' AND xtype='U') "
                    + "CREATE TABLE cloud_assets ("
                    + "id BIGINT IDENTITY(1,1) PRIMARY KEY, "
                    + "asset_name NVARCHAR(100) NOT NULL, "
                    + "asset_type NVARCHAR(30) NOT NULL, "
                    + "owner_name NVARCHAR(80) NOT NULL, "
                    + "status NVARCHAR(30) NOT NULL DEFAULT 'READY', "
                    + "description NVARCHAR(1000) NULL, "
                    + "created_at DATETIME2 DEFAULT SYSUTCDATETIME(), "
                    + "updated_at DATETIME2 DEFAULT SYSUTCDATETIME())";
        } else if (url.contains("postgresql")) {
            sql = "CREATE TABLE IF NOT EXISTS cloud_assets ("
                    + "id BIGSERIAL PRIMARY KEY, "
                    + "asset_name VARCHAR(100) NOT NULL, "
                    + "asset_type VARCHAR(30) NOT NULL, "
                    + "owner_name VARCHAR(80) NOT NULL, "
                    + "status VARCHAR(30) NOT NULL DEFAULT 'READY', "
                    + "description VARCHAR(1000), "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS cloud_assets ("
                    + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
                    + "asset_name VARCHAR(100) NOT NULL, "
                    + "asset_type VARCHAR(30) NOT NULL, "
                    + "owner_name VARCHAR(80) NOT NULL, "
                    + "status VARCHAR(30) NOT NULL DEFAULT 'READY', "
                    + "description VARCHAR(1000), "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)";
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void validateText(String value, String label) throws SQLException {
        if (trimToNull(value) == null) {
            throw new SQLException(label + "을 입력하세요.");
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
