# Pre-built on the host (`mvn -DskipTests package`) and copied in here rather than built
# inside this image. This machine's Docker network routes package-manager traffic through a
# corporate proxy that fails Maven Central/npm auth from inside the build context, so dependency
# resolution happens on the host (where it works) and Docker only packages the runtime image.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY target/app.jar /app/app.jar
USER app

# Override at runtime, e.g. ORCHESTRATION_ENGINE_URL=http://temporal-orchestration-workflow-engine:8081
ENV ORCHESTRATION_ENGINE_URL=http://localhost:8081

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
