<%@ page import="com.example.cloudstudy.config.AppConfigKeys" %>
<%@ page import="java.util.Map" %>
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
%>
<%
    Map<String, String> configValues = (Map<String, String>) request.getAttribute("configValues");
%>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>설정 관리</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/static/styles.css">
</head>
<body>
<main class="container">
    <nav class="nav">
        <a href="<%= request.getContextPath() %>/home">홈</a>
        <a href="<%= request.getContextPath() %>/status">서버 상태</a>
        <a href="<%= request.getContextPath() %>/api/config">API 보기</a>
    </nav>

    <section class="panel">
        <h1>외부 설정 파일 관리</h1>
        <p>
            입력한 값은 WAR 내부나 Tomcat 설치 디렉터리가 아니라 WAS 외부 설정 디렉터리의
            <code>application.properties</code> 파일에 저장됩니다.
        </p>

        <% if (request.getAttribute("message") != null) { %>
        <div class="alert success"><%= esc(request.getAttribute("message")) %></div>
        <% } %>

        <% if (request.getAttribute("error") != null) { %>
        <div class="alert error"><%= esc(request.getAttribute("error")) %></div>
        <% } %>

        <form method="post" action="<%= request.getContextPath() %>/config" class="form">
            <% for (String key : AppConfigKeys.ORDERED_KEYS) {
                boolean sensitive = AppConfigKeys.isSensitive(key);
                String value = configValues == null ? "" : configValues.getOrDefault(key, "");
            %>
            <label>
                <span><%= esc(key) %><%= sensitive ? " (민감값)" : "" %></span>
                <input
                        type="<%= sensitive ? "password" : "text" %>"
                        name="<%= esc(key) %>"
                        value="<%= sensitive ? "" : esc(value) %>"
                        placeholder="<%= sensitive && !value.isEmpty() ? "현재 저장값: " + esc(value) + " / 변경할 때만 입력" : "" %>">
            </label>
            <% } %>

            <button type="submit">저장</button>
        </form>

        <div class="note">
            <h2>현재 저장된 설정값</h2>
            <table>
                <tbody>
                <% for (String key : AppConfigKeys.ORDERED_KEYS) { %>
                <tr>
                    <th><%= esc(key) %></th>
                    <td><%= esc(configValues == null ? "" : configValues.getOrDefault(key, "")) %></td>
                </tr>
                <% } %>
                </tbody>
            </table>
        </div>

        <p class="path">
            설정 파일 최종 경로:
            <code><%= esc(request.getAttribute("configFilePath")) %></code>
        </p>
    </section>
</main>
</body>
</html>
