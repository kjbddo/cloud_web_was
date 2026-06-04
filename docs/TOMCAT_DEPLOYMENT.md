# Tomcat 9 배포 가이드

이 문서는 예시입니다. 애플리케이션 코드는 Tomcat 설치 디렉터리, Apache2 설치 디렉터리, Ubuntu의 특정 내부 경로에 의존하지 않습니다.

최종 배포물은 두 가지입니다.

```text
web-html/cloud-study-web/  -> Apache2 html 또는 DocumentRoot에 복사
target/cloud-study-app.war -> Tomcat 9 webapps에 복사
```

## 1. WAR 빌드

Windows PowerShell 또는 Linux shell에서 프로젝트 루트에서 실행합니다.

```bash
mvn clean package
```

빌드 결과:

```text
target/cloud-study-app.war
```

## 2. Tomcat webapps 배포

Tomcat 9의 `webapps` 디렉터리에 WAR를 복사합니다.

Windows 예시:

```powershell
Copy-Item .\target\cloud-study-app.war C:\tools\apache-tomcat-9\webapps\
```

Linux 예시:

```bash
sudo cp target/cloud-study-app.war /opt/tomcat/webapps/
```

위 경로는 예시입니다. 실제 Tomcat 설치 위치에 맞게 바꾸세요.

## 2-1. Apache2 html 폴더 배포

정적 화면은 Tomcat WAR가 아니라 Apache2에서 제공합니다.

Windows 예시:

```powershell
Copy-Item .\web-html\cloud-study-web C:\Apache24\htdocs\cloud-study-web -Recurse
```

Linux 예시:

```bash
sudo mkdir -p /var/www/html/cloud-study-web
sudo cp -r web-html/cloud-study-web/* /var/www/html/cloud-study-web/
```

위 경로는 예시입니다. 실제 Apache2 DocumentRoot에 맞게 변경하세요.

## 3. 외부 설정 디렉터리 지정

설정 파일 이름은 항상 `application.properties`입니다. 설정 디렉터리는 다음 우선순위로 결정됩니다.

1. 환경변수 `APP_CONFIG_DIR`
2. JVM 시스템 프로퍼티 `app.config.dir`
3. OS 사용자 홈 아래 `.cloud-study-app/config`

예를 들어 Linux에서 다음 경로를 사용할 수 있습니다.

```bash
/opt/cloud-study-app/config
```

이 경로는 배포 예시일 뿐이며 코드에 하드코딩되어 있지 않습니다.

## 4. APP_CONFIG_DIR 환경변수 설정

Windows PowerShell 예시:

```powershell
$env:APP_CONFIG_DIR = "C:\cloud-study-app\config"
```

Linux shell 예시:

```bash
export APP_CONFIG_DIR=/opt/cloud-study-app/config
```

Tomcat을 같은 shell에서 실행하면 해당 값을 읽습니다.

## 5. JVM 옵션으로 설정

환경변수 대신 JVM 옵션을 사용할 수 있습니다.

```bash
-Dapp.config.dir=/opt/cloud-study-app/config
```

인스턴스 식별값도 JVM 옵션으로 줄 수 있습니다.

```bash
-Dinstance.id=was-1
```

## 6. Tomcat setenv.sh 예시

Linux Tomcat에서 `bin/setenv.sh`를 사용하는 예시입니다.

```bash
#!/usr/bin/env bash

export APP_CONFIG_DIR=/opt/cloud-study-app/config
export INSTANCE_ID=was-1
export CATALINA_OPTS="$CATALINA_OPTS -Dfile.encoding=UTF-8"
```

실행 권한을 부여합니다.

```bash
chmod +x /opt/tomcat/bin/setenv.sh
```

Windows Tomcat은 `bin/setenv.bat`를 사용할 수 있습니다.

```bat
set APP_CONFIG_DIR=C:\cloud-study-app\config
set INSTANCE_ID=was-1
set CATALINA_OPTS=%CATALINA_OPTS% -Dfile.encoding=UTF-8
```

## 7. Ubuntu systemd 예시

Tomcat을 systemd 서비스로 실행한다면 환경변수를 서비스 파일에 넣을 수 있습니다.

```ini
[Unit]
Description=Apache Tomcat 9
After=network.target

[Service]
Type=forking
User=tomcat
Group=tomcat
Environment="JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64"
Environment="CATALINA_HOME=/opt/tomcat"
Environment="CATALINA_BASE=/opt/tomcat"
Environment="APP_CONFIG_DIR=/opt/cloud-study-app/config"
Environment="INSTANCE_ID=was-1"
Environment="CATALINA_OPTS=-Dfile.encoding=UTF-8"
ExecStart=/opt/tomcat/bin/startup.sh
ExecStop=/opt/tomcat/bin/shutdown.sh
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

권한 예시:

```bash
sudo mkdir -p /opt/cloud-study-app/config
sudo chown -R tomcat:tomcat /opt/cloud-study-app
sudo chmod 700 /opt/cloud-study-app/config
```

운영 환경에서는 설정 파일에 민감값이 들어가므로 WAS 실행 사용자만 읽고 쓸 수 있게 제한하세요.

## 8. 접속 확인

Tomcat 직접 접속:

```text
http://localhost:8080/cloud-study-app/api/health
http://localhost:8080/cloud-study-app/api/config
http://localhost:8080/cloud-study-app/api/server-status
```

Apache2 reverse proxy를 적용한 뒤:

```text
http://서버주소/
http://서버주소/config.html
http://서버주소/status.html
http://서버주소/api/health
```
