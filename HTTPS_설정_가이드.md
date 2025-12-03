# 🔒 HTTPS 설정 가이드

## 개요

모든 API 요청을 HTTPS로 암호화하여 보안을 강화합니다.

---

## ⚠️ 현재 상태

- **로컬 개발**: HTTP (http://127.0.0.1:8080)
- **서버 배포**: HTTP (http://210.104.76.139:8080)

---

## 🔐 HTTPS 적용 방법

### 방법 1: Spring Boot에 SSL 인증서 직접 설정

#### 1단계: SSL 인증서 발급

**Let's Encrypt 사용 (무료)**:

```bash
# 서버에서 실행
sudo apt update
sudo apt install certbot -y

# 인증서 발급 (도메인이 있는 경우)
sudo certbot certonly --standalone -d your-domain.com
```

또는 **자체 서명 인증서 (테스트용)**:

```bash
# 서버에서 실행
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /home/root/wiseyoung-backend/ssl/private.key \
  -out /home/root/wiseyoung-backend/ssl/certificate.crt
```

#### 2단계: Spring Boot 설정

`application-prod.yml`에 추가:

```yaml
server:
  port: 8443  # HTTPS 포트
  ssl:
    enabled: true
    key-store: /home/root/wiseyoung-backend/ssl/keystore.p12
    key-store-password: your-password
    key-store-type: PKCS12
    key-alias: wiseyoung
```

#### 3단계: 인증서를 PKCS12 형식으로 변환

```bash
# 서버에서 실행
sudo openssl pkcs12 -export -in certificate.crt -inkey private.key \
  -out keystore.p12 -name wiseyoung -password pass:your-password
```

---

### 방법 2: Nginx 리버스 프록시 사용 (권장)

#### 1단계: Nginx 설치

```bash
# 서버에서 실행
sudo apt update
sudo apt install nginx -y
```

#### 2단계: SSL 인증서 발급

```bash
sudo certbot --nginx -d your-domain.com
```

#### 3단계: Nginx 설정

`/etc/nginx/sites-available/wiseyoung`:

```nginx
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### 4단계: Nginx 활성화 및 재시작

```bash
sudo ln -s /etc/nginx/sites-available/wiseyoung /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

---

## 🔧 방화벽 설정

KT Cloud 콘솔에서:

1. **Inbound 규칙 추가**:
   - 포트: `443` (HTTPS)
   - 프로토콜: `TCP`
   - Source CIDR: `0.0.0.0/0` (모든 IP 허용)

2. **HTTP → HTTPS 리다이렉트** (선택사항):
   - 포트 `80`도 열어서 HTTPS로 리다이렉트

---

## 📱 안드로이드 앱 설정

HTTPS 적용 후 `NetworkModule.kt` 수정:

```kotlin
// HTTPS 사용
private const val BASE_URL = "https://your-domain.com/" // 또는 "https://210.104.76.139:8443/"
```

---

## ⚠️ 중요 사항

1. **도메인 필요**: Let's Encrypt는 도메인이 필요합니다
2. **인증서 갱신**: Let's Encrypt 인증서는 90일마다 갱신 필요
3. **자체 서명 인증서**: 테스트용으로만 사용 (브라우저 경고 발생)

---

## 🚀 빠른 테스트 (자체 서명 인증서)

로컬에서 테스트할 때만 사용:

```bash
# 자체 서명 인증서 생성
keytool -genkeypair -alias wiseyoung -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 365
```

---

**HTTPS 설정은 서버 배포 시 진행하시면 됩니다. 로컬 개발에서는 HTTP로 충분합니다.**

