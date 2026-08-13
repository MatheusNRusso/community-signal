# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S signal && adduser -S signal -G signal
COPY --from=builder /build/target/*.jar app.jar
RUN chown signal:signal app.jar
USER signal
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --retries=5 CMD wget -qO- http://localhost:8080/actuator/health 2>/dev/null || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
