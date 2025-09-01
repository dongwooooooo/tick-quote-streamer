# Multi-stage build for notification-service
FROM eclipse-temurin:21-jdk-jammy as builder

WORKDIR /app

# Gradle Wrapper 및 설정 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 소스 코드 복사 및 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon

# 실행 이미지
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# 보안을 위한 non-root 사용자 생성
RUN groupadd -r notification && useradd -r -g notification notification

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 파일 권한 설정
RUN chown notification:notification app.jar

# 비루트 사용자로 전환
USER notification

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8084/actuator/health || exit 1

# 포트 노출
EXPOSE 8084

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]

