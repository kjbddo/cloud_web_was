package com.example.cloudstudy.web;

import com.example.cloudstudy.config.AppConfigKeys;
import com.example.cloudstudy.config.ConfigStorageException;
import com.example.cloudstudy.config.ExternalConfigManager;
import com.example.cloudstudy.model.ServerStatus;
import com.example.cloudstudy.service.ConfigService;
import com.example.cloudstudy.service.DatabaseService;
import com.example.cloudstudy.service.ServerStatusService;
import com.example.cloudstudy.service.StorageService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiServlet extends HttpServlet {
    private final ExternalConfigManager configManager = new ExternalConfigManager();
    private final ConfigService configService = new ConfigService(configManager);
    private final DatabaseService databaseService = new DatabaseService(configManager);
    private final ServerStatusService serverStatusService = new ServerStatusService();
    private final StorageService storageService = new StorageService(configManager);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo() == null ? "" : request.getPathInfo();
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        if ("/health".equals(path)) {
            writeJson(response, health(request));
            return;
        }

        if ("/config".equals(path)) {
            writeJson(response, config());
            return;
        }

        if ("/server-status".equals(path)) {
            writeJson(response, serverStatus(request));
            return;
        }

        if ("/db/test".equals(path)) {
            writeJson(response, dbTest());
            return;
        }

        if ("/db/items".equals(path)) {
            writeJson(response, dbItems());
            return;
        }

        if ("/storage/blob/list".equals(path)) {
            writeJson(response, storageItems("blob"));
            return;
        }

        if ("/storage/file/list".equals(path)) {
            writeJson(response, storageItems("file"));
            return;
        }

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        writeJson(response, message("error", "지원하지 않는 API 경로입니다."));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String path = request.getPathInfo() == null ? "" : request.getPathInfo();
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        if ("/config".equals(path)) {
            saveConfig(request, response);
            return;
        }

        if (path.startsWith("/db/")) {
            handleDbPost(path, request, response);
            return;
        }

        if (path.startsWith("/storage/")) {
            handleStoragePost(path, request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        writeJson(response, message("error", "지원하지 않는 API 경로입니다."));
    }

    private void saveConfig(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");

        Map<String, String> values = new LinkedHashMap<>();
        for (String key : AppConfigKeys.ORDERED_KEYS) {
            values.put(key, request.getParameter(key));
        }

        try {
            configService.saveFromRequest(values);
            Map<String, Object> body = config();
            body.put("message", "설정 파일을 저장했습니다.");
            writeJson(response, body);
        } catch (ConfigStorageException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(response, message("error", e.getMessage()));
        }
    }

    private Map<String, Object> dbTest() {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            body.put("result", databaseService.testConnection());
        } catch (ConfigStorageException | SQLException e) {
            body.put("error", e.getMessage());
        }
        return body;
    }

    private Map<String, Object> dbItems() {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            body.put("items", databaseService.listItems());
        } catch (ConfigStorageException | SQLException e) {
            body.put("error", e.getMessage());
        }
        return body;
    }

    private void handleDbPost(String path, HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");

        try {
            if ("/db/items".equals(path)) {
                writeJson(response, databaseService.createItem(
                        request.getParameter("assetName"),
                        request.getParameter("assetType"),
                        request.getParameter("ownerName"),
                        request.getParameter("status"),
                        request.getParameter("description")));
                return;
            }

            if ("/db/items/update".equals(path)) {
                writeJson(response, databaseService.updateItem(
                        parseId(request),
                        request.getParameter("assetName"),
                        request.getParameter("assetType"),
                        request.getParameter("ownerName"),
                        request.getParameter("status"),
                        request.getParameter("description")));
                return;
            }

            if ("/db/items/delete".equals(path)) {
                writeJson(response, databaseService.deleteItem(parseId(request)));
                return;
            }

            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeJson(response, message("error", "지원하지 않는 DB API 경로입니다."));
        } catch (ConfigStorageException | SQLException | NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(response, message("error", e.getMessage()));
        }
    }

    private void handleStoragePost(String path, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try {
            if ("/storage/blob/upload".equals(path)) {
                Part file = request.getPart("file");
                writeJson(response, storageService.uploadBlob(file, request.getParameter("name")));
                return;
            }

            if ("/storage/blob/delete".equals(path)) {
                writeJson(response, storageService.deleteBlob(request.getParameter("name")));
                return;
            }

            if ("/storage/file/upload".equals(path)) {
                Part file = request.getPart("file");
                writeJson(response, storageService.uploadFile(file, request.getParameter("name")));
                return;
            }

            if ("/storage/file/delete".equals(path)) {
                writeJson(response, storageService.deleteFile(request.getParameter("name")));
                return;
            }

            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeJson(response, message("error", "지원하지 않는 스토리지 API 경로입니다."));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(response, message("error", e.getMessage()));
        }
    }

    private Map<String, Object> health(HttpServletRequest request) {
        ServerStatus status = serverStatusService.getStatus(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("instanceId", status.getInstanceId());
        body.put("hostName", status.getHostName());
        body.put("privateIp", status.getPrivateIp());
        body.put("currentTime", status.getCurrentTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return body;
    }

    private Map<String, Object> config() {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            body.put("configFilePath", configService.getPropertiesFilePath().toString());
            body.put("values", configService.loadMaskedConfig());
        } catch (ConfigStorageException e) {
            body.put("error", e.getMessage());
        }
        return body;
    }

    private Map<String, Object> serverStatus(HttpServletRequest request) {
        ServerStatus status = serverStatusService.getStatus(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("hostName", status.getHostName());
        body.put("privateIp", status.getPrivateIp());
        body.put("instanceId", status.getInstanceId());
        body.put("usedMemoryBytes", status.getUsedMemoryBytes());
        body.put("maxMemoryBytes", status.getMaxMemoryBytes());
        body.put("systemCpuLoad", status.getSystemCpuLoad());
        body.put("processCpuLoad", status.getProcessCpuLoad());
        body.put("currentTime", status.getCurrentTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        body.put("clientIp", status.getClientIp());
        body.put("xForwardedFor", status.getXForwardedFor());
        body.put("xForwardedProto", status.getXForwardedProto());
        body.put("xForwardedHost", status.getXForwardedHost());
        body.put("host", status.getHost());
        body.put("userAgent", status.getUserAgent());
        body.put("proxyOrLoadBalancerGuess", status.getProxyOrLoadBalancerGuess());
        return body;
    }

    private Map<String, Object> storageItems(String type) {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            if ("blob".equals(type)) {
                body.put("items", storageService.listBlobItems());
            } else {
                body.put("items", storageService.listFileItems());
            }
        } catch (Exception e) {
            body.put("error", e.getMessage());
        }
        return body;
    }

    private long parseId(HttpServletRequest request) {
        return Long.parseLong(request.getParameter("id"));
    }

    private Map<String, Object> message(String key, String value) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(key, value);
        return body;
    }

    private void writeJson(HttpServletResponse response, Map<String, Object> body) throws IOException {
        response.getWriter().write(JsonUtil.object(body));
    }
}
