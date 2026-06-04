-- Cloud Study App MySQL DDL example.
-- Azure Database for MySQL에서 먼저 실행한 뒤 /config.html의 db.endpoint에 DB 이름을 넣으세요.
--
-- 예:
-- db.endpoint=jdbc:mysql://kdh-mysql.mysql.database.azure.com:3306/cloud_study_app?useSSL=true&sslMode=REQUIRED&serverTimezone=UTC&allowPublicKeyRetrieval=true

CREATE DATABASE IF NOT EXISTS cloud_study_app
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE cloud_study_app;

CREATE TABLE IF NOT EXISTS cloud_assets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    asset_name VARCHAR(100) NOT NULL,
    asset_type VARCHAR(30) NOT NULL,
    owner_name VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'READY',
    description VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_cloud_assets_type (asset_type),
    INDEX idx_cloud_assets_status (status)
);

INSERT INTO cloud_assets (asset_name, asset_type, owner_name, status, description)
VALUES
    ('web-vm-01', 'WEB', 'cloud-team', 'RUNNING', 'Apache2 정적 웹 서버 예시'),
    ('was-vm-01', 'WAS', 'cloud-team', 'RUNNING', 'Tomcat 9 ROOT.war 배포 서버 예시'),
    ('mysql-flexible', 'DB', 'cloud-team', 'READY', 'Azure Database for MySQL 연결 실습용 예시')
ON DUPLICATE KEY UPDATE asset_name = asset_name;
