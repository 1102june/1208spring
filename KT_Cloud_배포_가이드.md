# KT Cloud 개발 서버 배포 가이드

## 📋 사전 준비 사항

### 1. KT Cloud 계정 및 인스턴스 준비
- KT Cloud 콘솔 접속
- 개발 서버 인스턴스 생성 (Ubuntu 20.04 LTS 권장)
- SSH 키 등록
- 보안 그룹 설정 (포트 8080, 3306, 22 오픈)

### 2. 필요한 도구 설치
```bash
# Java 17 설치
sudo apt update
sudo apt install openjdk-17-jdk -y

# MariaDB 설치
sudo apt install mariadb-server -y
sudo systemctl start mariadb
sudo systemctl enable mariadb

# Git 설치 (소스 관리용)
sudo apt install git -y

# Maven 설치 (또는 Gradle)
sudo apt install maven -y
```

### 3. 방화벽 설정
```bash
# UFW 방화벽 설정
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 8080/tcp  # Spring Boot
sudo ufw allow 3306/tcp  # MariaDB (선택, 외부 접근 필요 시)
sudo ufw enable
```

---

## 🗄️ 데이터베이스 설정

### 1. MariaDB 설정
```bash
# MariaDB 보안 설정
sudo mysql_secure_installation

# 데이터베이스 생성
sudo mysql -u root -p
```

```sql
-- 데이터베이스 생성
CREATE DATABASE wise_young CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 사용자 생성 (보안을 위해 root 사용 권장하지 않음)
CREATE USER 'wiseyoung'@'localhost' IDENTIFIED BY '강력한_비밀번호';
GRANT ALL PRIVILEGES ON wise_young.* TO 'wiseyoung'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

---

## 📦 백엔드 배포

### 방법 1: JAR 파일 배포 (권장)

#### 1. 로컬에서 JAR 빌드
```bash
cd C:\Users\subpa\IdeaProjects\WiseYoung_backend
./gradlew clean build -x test
```

빌드된 JAR 파일 위치: `build/libs/youth-0.0.1-SNAPSHOT.jar`

#### 2. 서버에 파일 업로드
```bash
# SCP로 JAR 파일 업로드 (Windows PowerShell)
scp -i "your-key.pem" build/libs/youth-0.0.1-SNAPSHOT.jar ubuntu@서버IP:/home/ubuntu/
```

#### 3. 서버에서 애플리케이션 실행
```bash
# 서버 접속
ssh -i "your-key.pem" ubuntu@서버IP

# 애플리케이션 디렉토리 생성
mkdir -p /home/ubuntu/wiseyoung-backend
cd /home/ubuntu/wiseyoung-backend

# JAR 파일 이동
mv ~/youth-0.0.1-SNAPSHOT.jar app.jar

# 환경 변수 설정 파일 생성
nano .env
```

`.env` 파일 내용:
```bash
# 데이터베이스 설정
DB_USERNAME=wiseyoung
DB_PASSWORD=강력한_비밀번호

# 이메일 설정
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-app-password

# 공공데이터 API 키
LH_SERVICE_KEY=your-lh-service-key
YOUTH_POLICY_SERVICE_KEY=your-youth-policy-key

# Gemini API 키
GEMINI_API_KEY=your-gemini-api-key

# 카카오맵 API 키
KAKAO_REST_API_KEY=your-kakao-key
```

#### 4. Systemd 서비스 생성 (자동 시작)
```bash
sudo nano /etc/systemd/system/wiseyoung-backend.service
```

서비스 파일 내용:
```ini
[Unit]
Description=WiseYoung Backend Service
After=network.target mariadb.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/wiseyoung-backend
EnvironmentFile=/home/ubuntu/wiseyoung-backend/.env
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod /home/ubuntu/wiseyoung-backend/app.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

#### 5. 서비스 시작
```bash
sudo systemctl daemon-reload
sudo systemctl enable wiseyoung-backend
sudo systemctl start wiseyoung-backend

# 상태 확인
sudo systemctl status wiseyoung-backend

# 로그 확인
sudo journalctl -u wiseyoung-backend -f
```

---

### 방법 2: Git으로 직접 배포

#### 1. 서버에 프로젝트 클론
```bash
cd /home/ubuntu
git clone https://github.com/your-repo/wiseyoung-backend.git
cd wiseyoung-backend
```

#### 2. 환경 변수 설정
```bash
# application-prod.yml 생성
nano src/main/resources/application-prod.yml
```

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/wise_young?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Seoul
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate  # 배포 환경에서는 validate 사용
server:
  port: 8080
```

#### 3. 빌드 및 실행
```bash
# 환경 변수 설정
export DB_USERNAME=wiseyoung
export DB_PASSWORD=your-password
export EMAIL_USERNAME=your-email@gmail.com
export EMAIL_PASSWORD=your-app-password
# ... 기타 환경 변수

# 빌드
./gradlew clean build -x test

# 실행
java -jar -Dspring.profiles.active=prod build/libs/youth-0.0.1-SNAPSHOT.jar
```

---

## 🔧 설정 파일 수정

### 1. application.yml 수정 (배포 환경)

`application-prod.yml` 생성 (또는 기존 파일 수정):
```yaml
spring:
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
    url: jdbc:mariadb://localhost:3306/wise_young?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Seoul
    username: ${DB_USERNAME:wiseyoung}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate  # ⚠️ 배포 시 validate로 변경
    show-sql: false  # 프로덕션에서는 false

server:
  port: 8080
  error:
    include-message: always
    include-binding-errors: always

logging:
  level:
    root: INFO
    com.example.youth: INFO
```

### 2. SecurityConfig에 CORS 추가 필요

`src/main/java/com/example/youth/config/SecurityConfig.java`에 CORS 설정 추가:
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:8080",
        "https://your-android-app-domain.com"  // 안드로이드 앱 도메인
    ));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

---

## 📱 안드로이드 앱 설정 변경

### NetworkModule.kt 수정
```kotlin
object NetworkModule {
    // 배포 서버 URL로 변경
    private const val BASE_URL = "http://서버IP:8080/"  // 또는 도메인 사용
    // 또는 HTTPS 사용 시
    // private const val BASE_URL = "https://your-domain.com/"
    
    // ... 나머지 코드 동일
}
```

---

## ✅ 배포 확인

### 1. 서버 상태 확인
```bash
# 서비스 상태
sudo systemctl status wiseyoung-backend

# 포트 확인
sudo netstat -tlnp | grep 8080

# 로그 확인
sudo journalctl -u wiseyoung-backend -n 100
```

### 2. API 테스트
```bash
# 헬스 체크
curl http://서버IP:8080/api/health

# 정책 목록 조회
curl http://서버IP:8080/api/policy/list
```

### 3. 데이터베이스 연결 확인
```bash
# MariaDB 접속
sudo mysql -u wiseyoung -p wise_young

# 테이블 확인
SHOW TABLES;
```

---

## 🔐 보안 체크리스트

- [ ] 방화벽 설정 완료
- [ ] 데이터베이스 비밀번호 강력하게 설정
- [ ] 환경 변수로 민감 정보 관리
- [ ] CORS 설정 적절히 구성
- [ ] HTTPS 설정 (권장)
- [ ] 정기적인 보안 업데이트
- [ ] 로그 파일 보안 관리
- [ ] 자동 백업 설정

---

## 📞 문제 해결

### 애플리케이션이 시작되지 않을 때
```bash
# 로그 확인
sudo journalctl -u wiseyoung-backend -n 100

# 포트 확인
sudo lsof -i :8080

# Java 프로세스 확인
ps aux | grep java
```

### 데이터베이스 연결 오류
```bash
# MariaDB 상태 확인
sudo systemctl status mariadb

# 연결 테스트
mysql -u wiseyoung -p -h localhost wise_young
```

### 포트 접근 불가
```bash
# 방화벽 확인
sudo ufw status

# 포트 열기
sudo ufw allow 8080/tcp
```

---

## 🔄 업데이트 절차

1. 새 버전 JAR 빌드
2. 서버에 JAR 파일 업로드
3. 기존 서비스 중지
4. 새 JAR 파일로 교체
5. 서비스 재시작

```bash
sudo systemctl stop wiseyoung-backend
# JAR 파일 교체
sudo systemctl start wiseyoung-backend
```

