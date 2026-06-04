# Web/WAS 배포 설정 가이드

이 문서는 실제 배포할 때 수정해야 하는 값을 Web 서버와 WAS 서버 기준으로 분리한 가이드입니다.

## 1. 최종 배포물

```text
web-html/cloud-study-web/  -> Web 서버 Apache2 html 또는 DocumentRoot에 복사
target/cloud-study-app.war -> WAS 서버 Tomcat 9 webapps에 ROOT.war로 복사 권장
```

ROOT.war로 배포하면 WAS API 주소가 단순해집니다.

```text
http://WAS_PRIVATE_IP:8080/api/health
```

## 2. Web 서버 Apache2 설정

예시 파일:

```text
docs/apache2-cloud-study-app.conf
```

Ubuntu 서버 적용 위치 예시:

```text
/etc/apache2/sites-available/reverse-proxy.conf
```

ROOT.war 기준 최소 설정:

```apache
<VirtualHost *:80>
    ServerName 20.249.208.159

    DocumentRoot /var/www/html

    <Directory /var/www/html/>
        Require all granted
        Options -Indexes
        DirectoryIndex index.html
    </Directory>

    ProxyRequests Off
    ProxyPreserveHost On
    ProxyAddHeaders On

    RequestHeader set X-Forwarded-Proto "http"
    RequestHeader set X-Forwarded-Host "%{Host}i"

    ProxyPass        /api/ http://WAS_PRIVATE_IP:8080/api/
    ProxyPassReverse /api/ http://WAS_PRIVATE_IP:8080/api/
</VirtualHost>
```

수정해야 하는 값:

- `ServerName`: Public IP 또는 도메인
- `DocumentRoot`: 정적 HTML 파일을 복사한 경로
- `<Directory>`: `DocumentRoot`와 같은 경로
- `ProxyPass`: WAS private IP, Tomcat 포트, WAR context path
- `ProxyPassReverse`: `ProxyPass`와 동일한 WAS 주소

## 3. 000-default.conf 문제 방지

`apache2ctl -S` 결과가 아래처럼 나오면 기본 사이트가 요청을 받고 있는 상태입니다.

```text
*:80 kdh-web-image.internal.cloudapp.net (/etc/apache2/sites-enabled/000-default.conf:1)
```

이 경우 `/api/health`가 Tomcat으로 가지 않고 Apache 정적 경로에서 404가 납니다.

수정:

```bash
sudo a2dissite 000-default.conf
sudo a2ensite reverse-proxy.conf
sudo apache2ctl configtest
sudo systemctl restart apache2
```

확인:

```bash
sudo apache2ctl -S
```

기대 결과:

```text
*:80 20.249.208.159 (/etc/apache2/sites-enabled/reverse-proxy.conf:1)
```

## 4. Apache2 모듈

Reverse proxy와 header 전달에 필요한 모듈:

```bash
sudo a2enmod proxy proxy_http headers
sudo apache2ctl configtest
sudo systemctl restart apache2
```

로드밸런싱 실습까지 할 경우:

```bash
sudo a2enmod proxy_balancer lbmethod_byrequests
```

## 5. Web 서버 정적 파일 배포

`web-html/cloud-study-web` 내부 파일을 Apache2 `DocumentRoot`에 복사합니다.

DocumentRoot를 `/var/www/html`로 쓸 경우:

```bash
sudo cp -r web-html/cloud-study-web/* /var/www/html/
```

필수 파일:

```text
/var/www/html/index.html
/var/www/html/config.html
/var/www/html/status.html
/var/www/html/db.html
/var/www/html/storage.html
/var/www/html/assets/app.js
/var/www/html/assets/styles.css
```

## 6. WAS 서버 Tomcat 배포

WAR 빌드:

```bash
mvn clean package
```

ROOT.war로 배포:

```bash
sudo cp target/cloud-study-app.war /opt/tomcat/webapps/ROOT.war
```

Tomcat 설치 경로는 서버마다 다를 수 있습니다. 실제 `webapps` 위치에 맞게 바꾸세요.

ROOT.war 배포 확인:

```bash
curl http://localhost:8080/api/health
```

Web 서버에서 WAS 직접 확인:

```bash
curl http://WAS_PRIVATE_IP:8080/api/health
```

Web 서버 Apache 프록시 확인:

```bash
curl http://localhost/api/health
```

## 7. application.properties 외부 설정

설정 파일은 WAR 내부, Tomcat 설치 디렉터리, Apache2 설치 디렉터리에 저장하지 않습니다.

설정 디렉터리 우선순위:

1. 환경변수 `APP_CONFIG_DIR`
2. JVM 시스템 프로퍼티 `app.config.dir`
3. OS 사용자 홈 아래 `.cloud-study-app/config`

파일명:

```text
application.properties
```

Ubuntu 예시:

```bash
sudo mkdir -p /opt/cloud-study-app/config
sudo chown -R tomcat:tomcat /opt/cloud-study-app
sudo chmod 700 /opt/cloud-study-app/config
```

Tomcat `setenv.sh` 예시:

```bash
#!/usr/bin/env bash

export APP_CONFIG_DIR=/opt/cloud-study-app/config
export INSTANCE_ID=was-1
export CATALINA_OPTS="$CATALINA_OPTS -Dfile.encoding=UTF-8"
```

## 8. /config.html에서 저장할 값

DB:

```properties
db.endpoint=jdbc:mysql://kdh-mysql.mysql.database.azure.com:3306/cloud_study_app?useSSL=true&sslMode=REQUIRED&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.username=appuser
db.password=비밀번호
```

Azure Database for MySQL은 SSL을 요구하는 경우가 많으므로 `sslMode=REQUIRED`를 포함하세요. 사용자명은 Azure 포털에 표시되는 값을 그대로 사용하고, 환경에 따라 `사용자명@서버명` 형식이 필요할 수 있습니다.

DB가 아직 없다면 먼저 아래 DDL을 실행하세요.

```text
docs/sql/cloud-study-mysql-ddl.sql
```

Azure Blob:

```properties
storage.account=스토리지계정명
storage.container=컨테이너명
storage.sas=?sv=...&sp=rlcw...&sig=...
```

Azure File Storage:

```properties
storage.file.share=파일공유명
storage.file.directory=uploads
```

WAS Base URL:

```properties
was.baseUrl=http://WAS_PRIVATE_IP:8080
```

민감값인 `db.password`, `storage.sas`는 화면과 API 조회에서 마스킹됩니다.

## 9. DB CRUD 확인

1. `/config.html`에서 DB 설정을 저장합니다.
2. `docs/sql/cloud-study-mysql-ddl.sql`을 실행해 `cloud_study_app` 데이터베이스와 `cloud_assets` 테이블을 생성합니다.
3. `/db.html`로 이동합니다.
4. `DB 연결 확인`을 누릅니다.
5. 리소스 이름, 유형, 소유자, 상태, 설명을 입력한 뒤 저장합니다.
6. 목록에서 `수정 폼`, `삭제`를 테스트합니다.

WAS API:

```text
GET  /api/db/test
GET  /api/db/items
POST /api/db/items
POST /api/db/items/update
POST /api/db/items/delete
```

MySQL 드라이버는 WAR에 포함되어 있습니다. PostgreSQL, SQL Server 등을 쓰면 해당 JDBC 드라이버를 추가해야 합니다.

## 10. 스토리지 확인

1. `/config.html`에서 스토리지 설정을 저장합니다.
2. `/storage.html`로 이동합니다.
3. Blob 영역에서 파일을 선택하고 업로드합니다.
4. 같은 이름으로 다시 업로드해 덮어쓰기를 확인합니다.
5. File Storage 영역도 같은 방식으로 확인합니다.

WAS API:

```text
GET  /api/storage/blob/list
POST /api/storage/blob/upload
POST /api/storage/blob/delete
GET  /api/storage/file/list
POST /api/storage/file/upload
POST /api/storage/file/delete
```

SAS 토큰에는 최소한 목록, 읽기, 생성, 쓰기, 삭제 권한이 필요합니다.

## 11. 장애 확인 순서

Apache 설정 문법:

```bash
sudo apache2ctl configtest
```

VirtualHost 확인:

```bash
sudo apache2ctl -S
```

WAS 직접 확인:

```bash
curl http://WAS_PRIVATE_IP:8080/api/health
```

Apache 프록시 확인:

```bash
curl http://localhost/api/health
```

외부 확인:

```bash
curl http://PUBLIC_IP/api/health
```

판단 기준:

- Apache HTML 404: Apache가 정적 파일을 못 찾거나 `/api/` 프록시 설정이 적용되지 않음
- 502 Bad Gateway: Apache가 WAS로 넘겼지만 WAS 주소, 포트, 방화벽, Tomcat 상태 문제
- JSON 오류: WAS까지 요청이 도착했고 애플리케이션 설정 또는 DB/스토리지 설정 문제
