package com.example.cloudstudy.web;

import com.example.cloudstudy.config.AppConfigKeys;
import com.example.cloudstudy.config.ConfigStorageException;
import com.example.cloudstudy.config.ExternalConfigManager;
import com.example.cloudstudy.service.ConfigService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigServlet extends HttpServlet {
    private final ConfigService configService = new ConfigService(new ExternalConfigManager());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showConfig(request, response, null, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        Map<String, String> values = new LinkedHashMap<>();
        for (String key : AppConfigKeys.ORDERED_KEYS) {
            values.put(key, request.getParameter(key));
        }

        try {
            configService.saveFromRequest(values);
            showConfig(request, response, "설정 파일을 저장했습니다.", null);
        } catch (ConfigStorageException e) {
            showConfig(request, response, null, e.getMessage());
        }
    }

    private void showConfig(HttpServletRequest request, HttpServletResponse response, String message, String error)
            throws ServletException, IOException {
        try {
            request.setAttribute("configValues", configService.loadMaskedConfig());
            request.setAttribute("configKeys", AppConfigKeys.ORDERED_KEYS);
            request.setAttribute("configFilePath", configService.getPropertiesFilePath().toString());
            request.setAttribute("message", message);
            request.setAttribute("error", error);
        } catch (ConfigStorageException e) {
            request.setAttribute("configValues", new LinkedHashMap<String, String>());
            request.setAttribute("configKeys", AppConfigKeys.ORDERED_KEYS);
            request.setAttribute("configFilePath", configService.getPropertiesFilePath().toString());
            request.setAttribute("error", e.getMessage());
        }

        request.getRequestDispatcher("/WEB-INF/jsp/config.jsp").forward(request, response);
    }
}
