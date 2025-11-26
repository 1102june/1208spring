# Docker 설정 가이드

이 프로젝트를 Docker로 실행하기 위한 가이드입니다.

## 📋 목차

1. [필수 요구사항](#필수-요구사항)
2. [빠른 시작](#빠른-시작)
3. [환경 변수 설정](#환경-변수-설정)
4. [Docker 명령어](#docker-명령어)
5. [문제 해결](#문제-해결)

## 필수 요구사항

- Docker Desktop (Windows/Mac) 또는 Docker Engine (Linux)
- Docker Compose v2.0 이상

## 빠른 시작

### 1. 환경 변수 파일 생성

프로젝트 루트에 `.env` 파일을 생성하고 필요한 환경 변수를 설정하세요:

```bash
# .env 파일 예시
DB_USERNAME=root
DB_PASSWORD=your_password
DB_NAME=wise_young

# Gemini API
GEMINI_API_KEY=your_gemini_api_key

# 이메일 설정
EMAIL_USERNAME=your_email@gmail.com
EMAIL_PASSWORD=your_app_password

# 공공데이터 API 키
LH_SERVICE_KEY=your_lh_service_key
LH_ENCODING_KEY=your_encoding_key
LH_DECODING_KEY=your_decoding_key

# 카카오맵 API
KAKAO_REST_API_KEY=your_kakao_api_key
```

### 2. Docker Compose로 실행

```bash
# 이미지 빌드 및 컨테이너 시작
docker-compose up -d --build

# 로그 확인
docker-compose logs -f app

# 상태 확인
docker-compose ps
```

### 3. 애플리케이션 접속

- 애플리케이션: http://localhost:8080
- MariaDB: localhost:3306

## 환경 변수 설정

### 방법 1: .env 파일 사용 (권장)

프로젝트 루트에 `.env` 파일을 생성하고 환경 변수를 설정하세요.

```bash
# .env 파일 생성
touch .env

# 파일 편집 (원하는 에디터 사용)
notepad .env  # Windows
nano .env     # Linux/Mac
```

### 방법 2: 환경 변수 직접 설정

```bash
# Windows PowerShell
$env:GEMINI_API_KEY="your_api_key"
$env:DB_PASSWORD="your_password"

# Linux/Mac
export GEMINI_API_KEY="your_api_key"
export DB_PASSWORD="your_password"
```

### 방법 3: docker-compose.yml 직접 수정

`docker-compose.yml`의 `environment` 섹션에서 직접 값을 설정할 수 있습니다 (보안상 권장하지 않음).

## Docker 명령어

### 기본 명령어

```bash
# 컨테이너 빌드 및 시작
docker-compose up -d --build

# 컨테이너 시작 (이미 빌드된 경우)
docker-compose up -d

# 컨테이너 중지
docker-compose stop

# 컨테이너 중지 및 제거
docker-compose down

# 컨테이너 중지, 제거 및 볼륨 삭제 (데이터 삭제됨!)
docker-compose down -v

# 로그 확인
docker-compose logs -f app          # 애플리케이션 로그
docker-compose logs -f mariadb      # 데이터베이스 로그
docker-compose logs -f              # 모든 로그

# 컨테이너 상태 확인
docker-compose ps

# 컨테이너 재시작
docker-compose restart app
```

### 개별 서비스 관리

```bash
# 애플리케이션만 재시작
docker-compose restart app

# 데이터베이스만 재시작
docker-compose restart mariadb

# 특정 서비스 로그만 확인
docker-compose logs -f app
```

### 데이터베이스 접속

```bash
# MariaDB 컨테이너에 접속
docker-compose exec mariadb mysql -u root -p

# 또는 직접 명령 실행
docker-compose exec mariadb mysql -u root -p${DB_PASSWORD} wise_young
```

### 애플리케이션 쉘 접속

```bash
# 애플리케이션 컨테이너에 접속
docker-compose exec app sh
```

## 문제 해결

### 1. 포트 충돌

포트 8080 또는 3306이 이미 사용 중인 경우:

```yaml
# docker-compose.yml에서 포트 변경
ports:
  - "8081:8080"  # 호스트 포트 변경
```

### 2. 데이터베이스 연결 실패

애플리케이션이 데이터베이스에 연결하지 못하는 경우:

```bash
# 데이터베이스 컨테이너 상태 확인
docker-compose ps mariadb

# 데이터베이스 로그 확인
docker-compose logs mariadb

# 데이터베이스가 준비될 때까지 대기 (healthcheck 확인)
docker-compose up -d mariadb
# 몇 초 대기 후
docker-compose up -d app
```

### 3. 빌드 실패

```bash
# 캐시 없이 다시 빌드
docker-compose build --no-cache

# 특정 서비스만 재빌드
docker-compose build --no-cache app
```

### 4. 환경 변수 적용 안 됨

```bash
# 컨테이너 재시작
docker-compose down
docker-compose up -d --build
```

### 5. 볼륨 데이터 초기화

```bash
# 주의: 모든 데이터가 삭제됩니다!
docker-compose down -v
docker-compose up -d
```

## 프로덕션 배포

프로덕션 환경에서는 다음 사항을 고려하세요:

1. **환경 변수 보안**: `.env` 파일을 Git에 커밋하지 마세요
2. **데이터베이스 백업**: 볼륨 데이터를 정기적으로 백업하세요
3. **리소스 제한**: `docker-compose.yml`에 리소스 제한 추가
4. **HTTPS**: 리버스 프록시(Nginx, Traefik) 사용
5. **모니터링**: 로그 및 헬스체크 모니터링 설정

## 추가 리소스

- [Docker 공식 문서](https://docs.docker.com/)
- [Docker Compose 공식 문서](https://docs.docker.com/compose/)
- [Spring Boot Docker 가이드](https://spring.io/guides/gs/spring-boot-docker/)

