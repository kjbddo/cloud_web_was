package com.example.cloudstudy.web;

import com.example.cloudstudy.service.ServerStatusService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class StatusServlet extends HttpServlet {
    private final ServerStatusService serverStatusService = new ServerStatusService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 운영 환경에서는 이 화면에 접근 제한을 두어 인프라 정보 노출을 막아야 한다.
        request.setAttribute("status", serverStatusService.getStatus(request));
        request.getRequestDispatcher("/WEB-INF/jsp/status.jsp").forward(request, response);
    }
}
