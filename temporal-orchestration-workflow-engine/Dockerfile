# Pre-built on the host (`mvn -DskipTests package`) and copied in here rather than built
# inside this image. This machine's Docker network routes package-manager traffic through a
# corporate proxy that fails Maven Central/npm auth from inside the build context, so dependency
# resolution happens on the host (where it works) and Docker only packages the runtime image.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY target/app.jar /app/app.jar
USER app

# Override at runtime, e.g. TEMPORAL_TARGET=temporal:7233, CREATE_ACCOUNT_SERVICE_URL=http://create-account:8083
ENV TEMPORAL_TARGET=127.0.0.1:7233 \
    CREATE_ACCOUNT_SERVICE_URL=http://localhost:8083

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
