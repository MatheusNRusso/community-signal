FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -q
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S signal && adduser -S signal -G signal
COPY --from=builder /build/target/*.jar app.jar
RUN chown signal:signal app.jar
USER signal
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
HEALTHCHECK --interval=30s --timeout=10s --retries=5 CMD wget -qO- http://localhost:8085/actuator/health 2>/dev/null || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
