<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cloud Study App</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/static/styles.css">
</head>
<body>
<main class="container">
    <section class="hero">
        <p class="eyebrow">Apache2 + Tomcat 9 + External Config</p>
        <h1>클라우드 엔지니어링 학습용 예제</h1>
        <p>
            Web 서버는 Apache2, WAS는 Tomcat 9로 분리하고 외부 설정 파일과 서버 상태를 화면에서 확인하는 학습용 WAR 프로젝트입니다.
        </p>
    </section>

    <section class="grid">
        <article class="card">
            <h2>설정 관리</h2>
            <p>DB 엔드포인트, 스토리지 SAS 토큰 등 백엔드 설정값을 외부 properties 파일에 저장합니다.</p>
            <a class="button" href="<%= request.getContextPath() %>/config">/config 열기</a>
        </article>

        <article class="card">
            <h2>서버 상태</h2>
            <p>요청을 처리한 인스턴스, private IP, CPU/메모리, 프록시 헤더를 확인합니다.</p>
            <a class="button" href="<%= request.getContextPath() %>/status">/status 열기</a>
        </article>

        <article class="card">
            <h2>API 테스트</h2>
            <p>Apache2 reverse proxy 또는 로드밸런서가 WAS로 요청을 전달하는지 JSON API로 확인합니다.</p>
            <a class="button secondary" href="<%= request.getContextPath() %>/api/health">/api/health 호출</a>
        </article>
    </section>
</main>
</body>
</html>
