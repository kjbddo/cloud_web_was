# Cloud Study App

Apache2 Web 서버와 Tomcat 9 WAS 서버 연동을 학습하기 위한 Java 11 예제입니다. 최종 배포물은 명확하게 두 가지입니다.

1. Apache2의 `html` 또는 DocumentRoot에 올릴 정적 웹 폴더: `web-html/cloud-study-web/`
2. Tomcat 9의 `webapps`에 올릴 WAR 파일: `target/cloud-study-app.war`

이 프로젝트는 운영용이 아니라 클라우드 엔지니어링 학습용입니다.

## 프로젝트 디렉터리 구조

```text
cloud_web_was/
├─ pom.xml
├─ README.md
├─ docs/
│  ├─ apache2-cloud-study-app.conf
│  └─ TOMCAT_DEPLOYMENT.md
├─ web-html/
│  └─ cloud-study-web/              # Apache2 html 내부에 올릴 폴더
│     ├─ index.html
│     ├─ config.html
│     ├─ status.html
│     └─ assets/
│        ├─ app.js
│        └─ styles.css
└─ src/
   └─ main/
      ├─ java/com/example/cloudstudy/
      │  ├─ config/
      │  ├─ model/
      │  ├─ service/
      │  └─ web/
      └─ webapp/                    # WAR 내부 리소스
```

## 배포 후 구조

```text
Apache2 Web Server
└─ html/
   └─ cloud-study-web/
      ├─ index.html
      ├─ config.html
      ├─ status.html
      └─ assets/

Tomcat 9 WAS Server
└─ webapps/
   └─ cloud-study-app.war

External Config Directory
└─ application.properties
```

Apache2는 HTML/CSS/JS를 제공합니다. 정적 HTML은 `/api/*` 요청만 Tomcat WAR로 호출합니다.

## 주요 URL

Apache2를 통해 접속하는 URL:

```text
http://서버주소/
http://서버주소/config.html
http://서버주소/status.html
http://서버주소/api/health
http://서버주소/api/config
http://서버주소/api/server-status
```

Tomcat 직접 접속 테스트 URL:

```text
http://localhost:8080/cloud-study-app/api/health
http://localhost:8080/cloud-study-app/api/config
http://localhost:8080/cloud-study-app/api/server-status
```

## 기능 요약

- `config.html`에서 DB 엔드포인트, DB 계정, 스토리지 계정, SAS 토큰, WAS Base URL을 입력합니다.
- 저장 버튼을 누르면 정적 HTML이 Tomcat WAR의 `POST /api/config`를 호출합니다.
- WAS는 외부 설정 디렉터리의 `application.properties`를 생성하거나 수정합니다.
- `db.password`, `storage.sas`는 조회 화면과 API 응답에서 마스킹됩니다.
- `status.html`은 `GET /api/server-status`를 호출해 현재 요청을 처리한 WAS 인스턴스 정보를 보여줍니다.
- Apache2 reverse proxy 또는 로드밸런서를 거치면 `X-Forwarded-*` 헤더를 화면에서 확인할 수 있습니다.

## 외부 설정 파일 위치

설정 파일명은 항상 `application.properties`입니다. 설정 디렉터리는 다음 우선순위로 결정됩니다.

1. 환경변수 `APP_CONFIG_DIR`
2. JVM 시스템 프로퍼티 `app.config.dir`
3. OS 사용자 홈 디렉터리 아래 `.cloud-study-app/config`

예시:

```text
Windows: C:\cloud-study-app\config\application.properties
Linux:   /opt/cloud-study-app/config/application.properties
Default: 사용자홈/.cloud-study-app/config/application.properties
```

`/opt/cloud-study-app/config`는 Ubuntu 배포 예시일 뿐이며 코드에 하드코딩되어 있지 않습니다.

## properties 예시

```properties
db.endpoint=jdbc:mysql://10.0.1.10:3306/app
db.username=appuser
db.password=secret
storage.account=mystorage
storage.container=uploads
storage.sas=?sv=...
was.baseUrl=http://localhost:8080/cloud-study-app
```

## Windows에서 실행하기

### 1. WAR 빌드

JDK 11과 Maven을 설치한 뒤 프로젝트 루트에서 실행합니다.

```powershell
mvn clean package
```

결과:

```text
target\cloud-study-app.war
```

### 2. 정적 웹 폴더 복사

Apache2 또는 로컬 웹 서버의 html 폴더 아래로 `web-html\cloud-study-web` 폴더 전체를 복사합니다.

```powershell
Copy-Item .\web-html\cloud-study-web C:\Apache24\htdocs\cloud-study-web -Recurse
```

Apache2의 DocumentRoot를 `C:\Apache24\htdocs\cloud-study-web`로 잡거나, 기존 DocumentRoot 아래 `cloud-study-web` 경로로 접근해도 됩니다.

### 3. WAR 배포

```powershell
Copy-Item .\target\cloud-study-app.war C:\tools\apache-tomcat-9\webapps\
```

Tomcat 경로는 예시입니다. 실제 설치 위치에 맞게 변경하세요.

### 4. 외부 설정 디렉터리 지정

```powershell
New-Item -ItemType Directory -Force C:\cloud-study-app\config
$env:APP_CONFIG_DIR = "C:\cloud-study-app\config"
```

Tomcat을 같은 PowerShell 세션에서 실행하면 이 환경변수를 읽습니다.

## Ubuntu Apache2 + Tomcat 배포하기

### 1. WAR 빌드

```bash
mvn clean package
```

### 2. 정적 웹 폴더 배포

```bash
sudo mkdir -p /var/www/html/cloud-study-web
sudo cp -r web-html/cloud-study-web/* /var/www/html/cloud-study-web/
```

`/var/www/html`은 Apache2의 일반적인 예시 경로입니다. 실제 DocumentRoot가 다르면 그 경로에 복사하세요.

### 3. WAR 배포

```bash
sudo cp target/cloud-study-app.war /opt/tomcat/webapps/
```

`/opt/tomcat`은 예시입니다. 실제 Tomcat 설치 위치에 맞게 바꾸세요.

### 4. 외부 설정 디렉터리 준비

```bash
sudo mkdir -p /opt/cloud-study-app/config
sudo chown -R tomcat:tomcat /opt/cloud-study-app
sudo chmod 700 /opt/cloud-study-app/config
```

Tomcat 실행 사용자만 읽고 쓸 수 있도록 권한을 제한하세요.

### 5. Tomcat 환경변수 설정

`setenv.sh` 예시:

```bash
#!/usr/bin/env bash

export APP_CONFIG_DIR=/opt/cloud-study-app/config
export INSTANCE_ID=was-1
export CATALINA_OPTS="$CATALINA_OPTS -Dfile.encoding=UTF-8"
```

JVM 옵션으로도 설정할 수 있습니다.

```bash
-Dapp.config.dir=/opt/cloud-study-app/config
-Dinstance.id=was-1
```

자세한 systemd 예시는 `docs/TOMCAT_DEPLOYMENT.md`를 참고하세요.

### 6. Apache2 reverse proxy 설정

예시 파일:

```text
docs/apache2-cloud-study-app.conf
```

Ubuntu 적용 예시:

```bash
sudo a2enmod proxy proxy_http headers
sudo cp docs/apache2-cloud-study-app.conf /etc/apache2/sites-available/cloud-study-app.conf
sudo a2ensite cloud-study-app.conf
sudo apache2ctl configtest
sudo systemctl reload apache2
```

이 Apache2 설정은 정적 HTML은 Apache2에서 직접 제공하고, `/api/`만 Tomcat으로 프록시합니다.

```apache
DocumentRoot /var/www/html/cloud-study-web
ProxyPass        /api/ http://127.0.0.1:8080/cloud-study-app/api/
ProxyPassReverse /api/ http://127.0.0.1:8080/cloud-study-app/api/
```

## Apache2와 WAS 통신 흐름

```text
브라우저
  -> Apache2: /index.html, /config.html, /status.html
  -> Apache2: /api/health
  -> Tomcat:  /cloud-study-app/api/health
  -> WAS가 JSON 응답
  -> Apache2가 브라우저에 응답 전달
```

Apache2는 다음 헤더를 WAS에 전달하도록 설정되어 있습니다.

- `X-Forwarded-For`
- `X-Forwarded-Proto`
- `X-Forwarded-Host`
- `Host`

## 서버 상태 확인

`status.html`은 다음 정보를 보여줍니다.

- 현재 WAS 서버 호스트명
- loopback이 아닌 private IPv4
- 인스턴스 식별값
- JVM 메모리
- 시스템 CPU 사용률
- JVM 프로세스 CPU 사용률
- 현재 시간
- 요청 클라이언트 IP
- `X-Forwarded-For`
- `X-Forwarded-Proto`
- `X-Forwarded-Host`
- `Host`
- `User-Agent`
- 프록시 또는 로드밸런서 경유 추정 여부

인스턴스 식별값 우선순위:

1. 환경변수 `INSTANCE_ID`
2. JVM 시스템 프로퍼티 `instance.id`
3. 호스트명

CPU 사용률을 가져올 수 없는 환경에서는 `N/A`로 표시합니다.

## 오토스케일링 확인 방법

1. 동일한 `cloud-study-app.war`를 여러 WAS 인스턴스에 배포합니다.
2. 각 WAS에 서로 다른 `INSTANCE_ID`를 설정합니다.
3. Apache2 balancer 또는 클라우드 로드밸런서가 여러 WAS로 `/api/` 요청을 분산하도록 구성합니다.
4. `status.html`을 반복 새로고침합니다.
5. `인스턴스 식별값`과 `Private IPv4`가 달라지는지 확인합니다.

## 로드밸런싱 확인 방법

`docs/apache2-cloud-study-app.conf` 하단에는 Apache2 balancer 예시가 주석으로 포함되어 있습니다.

확인 포인트:

- `status.html` 새로고침 시 `INSTANCE_ID`가 번갈아 표시되는지 확인합니다.
- `/api/health` 응답의 `instanceId`, `hostName`, `privateIp`가 여러 값으로 나타나는지 확인합니다.
- `X-Forwarded-For`가 표시되면 프록시 또는 로드밸런서를 거친 요청으로 추정합니다.

## 보안 주의사항

- 이 프로젝트는 학습용입니다.
- `application.properties`는 웹에서 직접 접근할 수 없는 외부 설정 디렉터리에 저장하세요.
- Linux에서는 설정 디렉터리를 WAS 실행 사용자만 읽고 쓸 수 있게 제한하세요.
- SAS 토큰, DB 비밀번호는 화면과 API에서 마스킹되지만 파일에는 원문으로 저장됩니다.
- 운영 환경에서는 비밀값을 Key Vault, Secrets Manager, Vault 같은 전용 비밀 관리 서비스에 저장하세요.
- `status.html`은 인프라 정보를 노출하므로 운영 환경에서는 접근 제한이 필요합니다.
- `ADMIN_USERNAME`, `ADMIN_PASSWORD` 환경변수를 설정하면 `/api/config`, `/api/server-status`에 Basic 인증이 적용됩니다.

## 최종 산출물

빌드 후 실제로 배포할 것은 아래 두 개입니다.

```text
web-html/cloud-study-web/       -> Apache2 html 또는 DocumentRoot에 복사
target/cloud-study-app.war      -> Tomcat 9 webapps에 복사
```
# Cloud Study App

Apache2 Web 서버와 Tomcat 9 WAS 서버 연동을 학습하기 위한 Java 11 Servlet/JSP WAR 예제입니다. 실제 운영용이 아니라 클라우드 엔지니어링 실습용 프로젝트입니다.

## 프로젝트 디렉터리 구조

```text
cloud_web_was/
├─ pom.xml
├─ README.md
├─ docs/
│  ├─ apache2-cloud-study-app.conf
│  └─ TOMCAT_DEPLOYMENT.md
└─ src/
   └─ main/
      ├─ java/
      │  └─ com/example/cloudstudy/
      │     ├─ config/
      │     │  ├─ AppConfigKeys.java
      │     │  ├─ ConfigStorageException.java
      │     │  └─ ExternalConfigManager.java
      │     ├─ model/
      │     │  └─ ServerStatus.java
      │     ├─ service/
      │     │  ├─ ConfigService.java
      │     │  └─ ServerStatusService.java
      │     └─ web/
      │        ├─ ApiServlet.java
      │        ├─ BasicAuthFilter.java
      │        ├─ ConfigServlet.java
      │        ├─ HomeServlet.java
      │        ├─ JsonUtil.java
      │        └─ StatusServlet.java
      └─ webapp/
         ├─ index.jsp
         ├─ static/
         │  └─ styles.css
         └─ WEB-INF/
            ├─ web.xml
            └─ jsp/
               ├─ config.jsp
               ├─ index.jsp
               └─ status.jsp
```

## 전체 구조

- Apache2는 80 포트에서 정적 파일을 제공하고, `/api`, `/config`, `/status`, `/home` 요청을 Tomcat WAS로 reverse proxy합니다.
- Tomcat 9는 8080 포트에서 `cloud-study-app.war`를 실행합니다.
- 설정 파일은 WAR 내부, Tomcat 설치 디렉터리, Apache2 설치 디렉터리에 저장하지 않습니다.
- 설정 파일은 외부 설정 디렉터리의 `application.properties`로 저장됩니다.
- `/config` 화면은 설정 저장/조회용 화면입니다.
- `/status` 화면은 현재 요청을 처리한 WAS 인스턴스와 프록시 헤더를 확인하는 화면입니다.
- `/api/health`, `/api/config`, `/api/server-status`는 Apache2와 WAS 연동 확인용 API입니다.

## 외부 설정 디렉터리 결정 우선순위

설정 디렉터리는 다음 순서로 결정됩니다.

1. 환경변수 `APP_CONFIG_DIR`
2. JVM 시스템 프로퍼티 `app.config.dir`
3. OS 사용자 홈 디렉터리 아래 `.cloud-study-app/config`

최종 파일명은 항상 `application.properties`입니다.

예시:

```text
Windows: C:\cloud-study-app\config\application.properties
Linux:   /opt/cloud-study-app/config/application.properties
Default: 사용자홈/.cloud-study-app/config/application.properties
```

Linux의 `/opt/cloud-study-app/config`는 배포 예시일 뿐이며 코드에 하드코딩되어 있지 않습니다.

## 저장되는 properties 예시

```properties
db.endpoint=jdbc:mysql://10.0.1.10:3306/app
db.username=appuser
db.password=secret
storage.account=mystorage
storage.container=uploads
storage.sas=?sv=...
was.baseUrl=http://localhost:8080/cloud-study-app
```

`db.password`, `storage.sas`는 화면과 API 조회 시 마스킹됩니다.

## 실행 순서

### 1. 필요 도구 설치

- JDK 11
- Maven
- Tomcat 9
- Apache2, Linux reverse proxy 실습 시 필요

### 2. WAR 빌드

프로젝트 루트에서 실행합니다.

```bash
mvn clean package
```

결과 파일:

```text
target/cloud-study-app.war
```

### 3. 외부 설정 디렉터리 준비

Windows PowerShell 예시:

```powershell
New-Item -ItemType Directory -Force C:\cloud-study-app\config
$env:APP_CONFIG_DIR = "C:\cloud-study-app\config"
```

Linux 예시:

```bash
sudo mkdir -p /opt/cloud-study-app/config
sudo chown -R tomcat:tomcat /opt/cloud-study-app
sudo chmod 700 /opt/cloud-study-app/config
export APP_CONFIG_DIR=/opt/cloud-study-app/config
```

디렉터리가 없으면 애플리케이션이 WAS 실행 사용자 권한으로 자동 생성합니다. 권한이 없으면 `/config` 화면에 이해하기 쉬운 오류 메시지를 표시합니다.

### 4. Tomcat 배포

Windows 예시:

```powershell
Copy-Item .\target\cloud-study-app.war C:\tools\apache-tomcat-9\webapps\
```

Linux 예시:

```bash
sudo cp target/cloud-study-app.war /opt/tomcat/webapps/
```

Tomcat 설치 경로는 환경마다 다르므로 위 경로는 예시로만 사용하세요.

### 5. JVM 옵션 방식

환경변수 대신 JVM 옵션을 사용할 수 있습니다.

```bash
-Dapp.config.dir=/opt/cloud-study-app/config
-Dinstance.id=was-1
```

자세한 `setenv.sh`, `setenv.bat`, systemd 예시는 `docs/TOMCAT_DEPLOYMENT.md`를 참고하세요.

### 6. Tomcat 직접 테스트

```text
http://localhost:8080/cloud-study-app/
http://localhost:8080/cloud-study-app/config
http://localhost:8080/cloud-study-app/status
http://localhost:8080/cloud-study-app/api/health
http://localhost:8080/cloud-study-app/api/config
http://localhost:8080/cloud-study-app/api/server-status
```

## Apache2와 Tomcat 연동 방식

예시 설정 파일은 `docs/apache2-cloud-study-app.conf`입니다.

Ubuntu 기준 예시 위치:

```bash
/etc/apache2/sites-available/cloud-study-app.conf
```

활성화 예시:

```bash
sudo a2enmod proxy proxy_http headers
sudo cp docs/apache2-cloud-study-app.conf /etc/apache2/sites-available/cloud-study-app.conf
sudo a2ensite cloud-study-app.conf
sudo apache2ctl configtest
sudo systemctl reload apache2
```

Apache2 설정 예시는 다음 헤더를 WAS로 전달합니다.

- `X-Forwarded-For`
- `X-Forwarded-Proto`
- `X-Forwarded-Host`
- `Host`

Apache2를 거친 뒤 접속 예시:

```text
http://서버주소/
http://서버주소/config
http://서버주소/status
http://서버주소/api/health
```

## 외부 properties 생성/수정 흐름

1. 사용자가 `/config` 화면에서 설정값을 입력합니다.
2. `ConfigServlet`이 입력값을 받습니다.
3. `ConfigService`가 기존 properties를 읽고 입력값을 병합합니다.
4. `ExternalConfigManager`가 설정 디렉터리를 결정합니다.
5. 디렉터리가 없으면 자동 생성합니다.
6. `application.properties`에 저장합니다.
7. 저장 후 화면에 마스킹된 설정값과 최종 파일 경로를 표시합니다.

민감값은 조회 화면에 원문으로 표시하지 않습니다. 민감값 입력칸을 비워 저장하면 기존 값이 유지됩니다.

## 서버 상태 조회 흐름

`/status` 화면과 `/api/server-status`는 다음 정보를 표시합니다.

- 현재 WAS 서버 호스트명
- loopback이 아닌 private IPv4 우선 표시
- 인스턴스 식별값
- JVM 메모리 사용량
- 시스템 CPU 사용률
- JVM 프로세스 CPU 사용률
- 현재 시간
- 요청 클라이언트 IP
- `X-Forwarded-For`, `X-Forwarded-Proto`, `X-Forwarded-Host`
- `Host`, `User-Agent`
- 프록시 또는 로드밸런서 경유 추정 여부

인스턴스 식별값 우선순위:

1. 환경변수 `INSTANCE_ID`
2. JVM 시스템 프로퍼티 `instance.id`
3. 호스트명

CPU 사용률은 `com.sun.management.OperatingSystemMXBean`으로 조회합니다. 환경에서 지원하지 않으면 `N/A`로 표시합니다.

로드밸런서 여부는 확정하지 않고 추정만 합니다. `X-Forwarded-For` 헤더가 있으면 프록시 또는 로드밸런서를 거친 요청으로 추정하고, 없으면 직접 요청 또는 헤더 미전달 상태로 표시합니다.

## 오토스케일링 확인 방법

1. 동일한 WAR를 여러 WAS 인스턴스에 배포합니다.
2. 각 인스턴스에 서로 다른 `INSTANCE_ID`를 설정합니다.
3. 로드밸런서 또는 Apache2 balancer가 여러 WAS로 요청을 분산하도록 구성합니다.
4. `/status` 화면을 반복 새로고침합니다.
5. `인스턴스 식별값`과 `Private IPv4`가 바뀌는지 확인합니다.

오토스케일링 실습에서는 인스턴스 수를 늘리거나 줄인 뒤 새 인스턴스가 `/api/health`에 정상 응답하는지 확인합니다.

## 로드밸런싱 확인 방법

Apache2 balancer 예시는 `docs/apache2-cloud-study-app.conf` 하단 주석에 포함되어 있습니다.

확인 포인트:

- `/status` 새로고침 시 `INSTANCE_ID`가 번갈아 표시되는지 확인합니다.
- `/api/health` 응답의 `instanceId`, `hostName`, `privateIp`가 여러 값으로 나타나는지 확인합니다.
- `X-Forwarded-For`가 표시되면 프록시 또는 로드밸런서를 거친 요청으로 추정할 수 있습니다.

## 간단 인증

`/config`, `/status`, `/api/config`, `/api/server-status`는 민감한 설정 또는 인프라 정보를 보여줄 수 있습니다. 이 예제는 다음 환경변수가 설정된 경우 Basic 인증을 적용합니다.

```bash
ADMIN_USERNAME=admin
ADMIN_PASSWORD=change-me
```

학습 편의를 위해 두 값이 없으면 인증 없이 동작합니다. 운영 환경에서는 반드시 인증, VPN, 보안 그룹, 방화벽, 사내망 제한 같은 접근 통제를 적용하세요.

## 보안 주의사항

- 이 프로젝트는 운영용이 아닌 학습용입니다.
- `application.properties`는 웹에서 직접 접근할 수 없는 외부 설정 디렉터리에 저장하세요.
- Linux에서는 설정 디렉터리를 WAS 실행 사용자만 읽고 쓸 수 있게 `chmod 700` 수준으로 제한하세요.
- SAS 토큰, DB 비밀번호는 화면과 API에서 마스킹되지만 파일에는 원문으로 저장됩니다.
- 실제 운영에서는 비밀값을 Key Vault, Secrets Manager, Vault 같은 전용 비밀 관리 서비스에 저장하는 방식을 권장합니다.
- `/status`는 서버 내부 정보를 노출하므로 운영 환경에서는 접근 제한이 필요합니다.

## 주요 파일 역할

- `ExternalConfigManager.java`: 외부 설정 디렉터리 우선순위 결정, 디렉터리 생성, properties 읽기/쓰기
- `ConfigService.java`: 설정값 저장과 민감값 마스킹
- `ServerStatusService.java`: 호스트/IP/CPU/메모리/프록시 헤더 조회
- `ConfigServlet.java`: `/config` 화면 처리
- `StatusServlet.java`: `/status` 화면 처리
- `ApiServlet.java`: `/api/health`, `/api/config`, `/api/server-status`
- `BasicAuthFilter.java`: 선택적 Basic 인증
- `docs/apache2-cloud-study-app.conf`: Apache2 reverse proxy와 정적 파일 제공 예시
- `docs/TOMCAT_DEPLOYMENT.md`: Tomcat 배포와 환경변수/JVM 옵션 설정 예시
