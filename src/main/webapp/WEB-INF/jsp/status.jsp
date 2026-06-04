<%@ page import="com.example.cloudstudy.model.ServerStatus" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%!
    private String esc(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String mb(long bytes) {
        return String.format("%.2f MB", bytes / 1024.0 / 1024.0);
    }
%>
<%
    ServerStatus status = (ServerStatus) request.getAttribute("status");
%>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>서버 상태</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/static/styles.css">
</head>
<body>
<main class="container">
    <nav class="nav">
        <a href="<%= request.getContextPath() %>/home">홈</a>
        <a href="<%= request.getContextPath() %>/config">설정 관리</a>
        <a href="<%= request.getContextPath() %>/api/server-status">API 보기</a>
    </nav>

    <section class="panel">
        <h1>현재 WAS 서버 상태</h1>
        <p>
            오토스케일링이나 로드밸런싱 테스트 시 새로고침하면서 인스턴스 식별값과 private IP가 바뀌는지 확인하세요.
            운영 환경에서는 이 화면이 민감한 인프라 정보를 노출할 수 있으므로 접근 제한이 필요합니다.
        </p>

        <table>
            <tbody>
            <tr><th>호스트명</th><td><%= esc(status.getHostName()) %></td></tr>
            <tr><th>Private IPv4</th><td><%= esc(status.getPrivateIp()) %></td></tr>
            <tr><th>인스턴스 식별값</th><td class="highlight"><%= esc(status.getInstanceId()) %></td></tr>
            <tr><th>JVM 사용 메모리</th><td><%= esc(mb(status.getUsedMemoryBytes())) %> / <%= esc(mb(status.getMaxMemoryBytes())) %></td></tr>
            <tr><th>시스템 CPU 사용률</th><td><%= esc(status.getSystemCpuLoad()) %></td></tr>
            <tr><th>JVM 프로세스 CPU 사용률</th><td><%= esc(status.getProcessCpuLoad()) %></td></tr>
            <tr><th>현재 시간</th><td><%= esc(status.getCurrentTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)) %></td></tr>
            <tr><th>요청 클라이언트 IP</th><td><%= esc(status.getClientIp()) %></td></tr>
            <tr><th>X-Forwarded-For</th><td><%= esc(status.getXForwardedFor()) %></td></tr>
            <tr><th>X-Forwarded-Proto</th><td><%= esc(status.getXForwardedProto()) %></td></tr>
            <tr><th>X-Forwarded-Host</th><td><%= esc(status.getXForwardedHost()) %></td></tr>
            <tr><th>Host</th><td><%= esc(status.getHost()) %></td></tr>
            <tr><th>User-Agent</th><td><%= esc(status.getUserAgent()) %></td></tr>
            <tr><th>프록시/로드밸런서 경유 추정</th><td><%= esc(status.getProxyOrLoadBalancerGuess()) %></td></tr>
            </tbody>
        </table>

        <div class="actions">
            <a class="button" href="<%= request.getContextPath() %>/status">새로고침</a>
            <a class="button secondary" href="<%= request.getContextPath() %>/api/health">Health API</a>
        </div>
    </section>
</main>
</body>
</html>
