# 멀티 스테이지 빌드를 사용한 최적화된 Dockerfile

# Stage 1: 빌드 스테이지
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Gradle 캐시를 활용하기 위해 의존성 파일 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 활용)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY src ./src
RUN gradle clean bootJar --no-daemon

# Stage 2: 실행 스테이지
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 헬스체크를 위한 wget 설치 (root 권한 필요)
RUN apk add --no-cache wget

# 보안을 위한 non-root 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring

# 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 파일 소유권 변경
RUN chown spring:spring app.jar

# non-root 사용자로 전환
USER spring:spring

# 포트 노출
EXPOSE 8080

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]

