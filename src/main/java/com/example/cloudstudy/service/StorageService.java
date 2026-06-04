package com.example.cloudstudy.service;

import com.example.cloudstudy.config.AppConfigKeys;
import com.example.cloudstudy.config.ConfigStorageException;
import com.example.cloudstudy.config.ExternalConfigManager;

import javax.servlet.http.Part;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class StorageService {
    private final ExternalConfigManager configManager;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public StorageService(ExternalConfigManager configManager) {
        this.configManager = configManager;
    }

    public List<Map<String, Object>> listBlobItems() throws Exception {
        StorageConfig config = loadConfig();
        URI uri = URI.create(config.blobContainerUrl() + appendSas("?restype=container&comp=list", config.sas));
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureSuccess(response.statusCode(), response.body(), "Blob 목록 조회에 실패했습니다.");
        return parseStorageNames(response.body(), "Blob");
    }

    public Map<String, Object> uploadBlob(Part filePart, String requestedName) throws Exception {
        StorageConfig config = loadConfig();
        byte[] bytes = filePart.getInputStream().readAllBytes();
        String fileName = safeFileName(requestedName, filePart);
        URI uri = URI.create(config.blobContainerUrl() + "/" + encodePath(fileName) + appendSas("?", config.sas));

        HttpRequest request = HttpRequest.newBuilder(uri)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .header("x-ms-blob-type", "BlockBlob")
                .header("Content-Type", contentType(filePart))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureSuccess(response.statusCode(), response.body(), "Blob 업로드에 실패했습니다.");
        return message("Blob 파일을 업로드 또는 덮어쓰기했습니다.", fileName, bytes.length);
    }

    public Map<String, Object> deleteBlob(String name) throws Exception {
        StorageConfig config = loadConfig();
        URI uri = URI.create(config.blobContainerUrl() + "/" + encodePath(name) + appendSas("?", config.sas));
        HttpRequest request = HttpRequest.newBuilder(uri).DELETE().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureSuccess(response.statusCode(), response.body(), "Blob 삭제에 실패했습니다.");
        return message("Blob 파일을 삭제했습니다.", name, 0);
    }

    public List<Map<String, Object>> listFileItems() throws Exception {
        StorageConfig config = loadConfig();
        URI uri = URI.create(config.fileDirectoryUrl() + appendSas("?restype=directory&comp=list", config.sas));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .header("x-ms-version", "2021-12-02")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureSuccess(response.statusCode(), response.body(), "File Storage 목록 조회에 실패했습니다.");
        return parseStorageNames(response.body(), "File");
    }

    public Map<String, Object> uploadFile(Part filePart, String requestedName) throws Exception {
        StorageConfig config = loadConfig();
        byte[] bytes = filePart.getInputStream().readAllBytes();
        String fileName = safeFileName(requestedName, filePart);
        String fileUrl = config.fileDirectoryUrl() + "/" + encodePath(fileName);

        HttpRequest createRequest = HttpRequest.newBuilder(URI.create(fileUrl + appendSas("?", config.sas)))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .header("x-ms-version", "2021-12-02")
                .header("x-ms-type", "file")
                .header("x-ms-content-length", String.valueOf(bytes.length))
                .build();
        HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureSuccess(createResponse.statusCode(), createResponse.body(), "File Storage 파일 생성에 실패했습니다.");

        if (bytes.length > 0) {
            HttpRequest rangeRequest = HttpRequest.newBuilder(URI.create(fileUrl + appendSas("?comp=range", config.sas)))
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .header("x-ms-version", "2021-12-02")
                    .header("x-ms-write", "update")
                    .header("x-ms-range", "bytes=0-" + (bytes.length - 1))
                    .build();
            HttpResponse<String> rangeResponse = httpClient.send(rangeRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            ensureSuccess(rangeResponse.statusCode(), rangeResponse.body(), "File Storage 파일 내용 업로드에 실패했습니다.");
        }

        return message("File Storage 파일을 업로드 또는 덮어쓰기했습니다.", fileName, bytes.length);
    }

    public Map<String, Object> deleteFile(String name) throws Exception {
        StorageConfig config = loadConfig();
        URI uri = URI.create(config.fileDirectoryUrl() + "/" + encodePath(name) + appendSas("?", config.sas));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .DELETE()
                .header("x-ms-version", "2021-12-02")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureSuccess(response.statusCode(), response.body(), "File Storage 삭제에 실패했습니다.");
        return message("File Storage 파일을 삭제했습니다.", name, 0);
    }

    private StorageConfig loadConfig() throws ConfigStorageException {
        Properties properties = configManager.load();
        return new StorageConfig(
                required(properties, AppConfigKeys.STORAGE_ACCOUNT),
                required(properties, AppConfigKeys.STORAGE_CONTAINER),
                required(properties, AppConfigKeys.STORAGE_SAS),
                trimToEmpty(properties.getProperty(AppConfigKeys.STORAGE_FILE_SHARE)),
                trimToEmpty(properties.getProperty(AppConfigKeys.STORAGE_FILE_DIRECTORY))
        );
    }

    private List<Map<String, Object>> parseStorageNames(String xml, String type) throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        NodeList names = document.getElementsByTagName("Name");
        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 0; i < names.getLength(); i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", names.item(i).getTextContent());
            item.put("type", type);
            result.add(item);
        }

        return result;
    }

    private Map<String, Object> message(String message, String name, long size) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", message);
        result.put("name", name);
        result.put("size", size);
        return result;
    }

    private void ensureSuccess(int statusCode, String body, String message) throws IOException {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        throw new IOException(message + " HTTP " + statusCode + " " + body);
    }

    private String required(Properties properties, String key) throws ConfigStorageException {
        String value = trimToNull(properties.getProperty(key));
        if (value == null) {
            throw new ConfigStorageException(key + " 설정이 비어 있습니다. /config.html에서 저장하세요.", null);
        }
        return value;
    }

    private String safeFileName(String requestedName, Part filePart) throws IOException {
        String name = trimToNull(requestedName);
        if (name == null) {
            name = trimToNull(filePart.getSubmittedFileName());
        }
        if (name == null) {
            throw new IOException("업로드할 파일명을 확인할 수 없습니다.");
        }
        return name.replace("\\", "/").replaceAll("^/+", "");
    }

    private String contentType(Part part) {
        String contentType = part.getContentType();
        return contentType == null ? "application/octet-stream" : contentType;
    }

    private String appendSas(String queryPrefix, String sas) {
        String token = sas.startsWith("?") ? sas.substring(1) : sas;
        if ("?".equals(queryPrefix)) {
            return "?" + token;
        }
        return queryPrefix + "&" + token;
    }

    private String encodePath(String value) {
        String[] parts = value.split("/");
        List<String> encoded = new ArrayList<>();
        for (String part : parts) {
            encoded.add(URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return String.join("/", encoded);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToEmpty(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed;
    }

    private static class StorageConfig {
        private final String account;
        private final String blobContainer;
        private final String sas;
        private final String fileShare;
        private final String fileDirectory;

        private StorageConfig(String account, String blobContainer, String sas, String fileShare, String fileDirectory) {
            this.account = account;
            this.blobContainer = blobContainer;
            this.sas = sas;
            this.fileShare = fileShare;
            this.fileDirectory = fileDirectory;
        }

        private String blobContainerUrl() {
            return "https://" + account + ".blob.core.windows.net/" + encodeStatic(blobContainer);
        }

        private String fileDirectoryUrl() throws ConfigStorageException {
            if (fileShare == null || fileShare.isEmpty()) {
                throw new ConfigStorageException("storage.file.share 설정이 비어 있습니다. /config.html에서 저장하세요.", null);
            }

            String url = "https://" + account + ".file.core.windows.net/" + encodeStatic(fileShare);
            if (fileDirectory != null && !fileDirectory.isEmpty()) {
                String[] parts = fileDirectory.replace("\\", "/").replaceAll("^/+", "").split("/");
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        url += "/" + encodeStatic(part);
                    }
                }
            }
            return url;
        }

        private String encodeStatic(String value) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
        }
    }
}
