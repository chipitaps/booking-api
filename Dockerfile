FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src src
RUN ./mvnw -B -ntp package -DskipTests \
    && java -Djarmode=tools -jar target/booking-api-*.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /app/extracted/dependencies/ ./
COPY --from=build --chown=app:app /app/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /app/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /app/extracted/application/ ./
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness >/dev/null 2>&1 || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
