# Pre-built on the host (`mvn -DskipTests package`) and copied in here rather than built
# inside this image. This machine's Docker network routes package-manager traffic through a
# corporate proxy that fails Maven Central/npm auth from inside the build context, so dependency
# resolution happens on the host (where it works) and Docker only packages the runtime image.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/app.jar /app/app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
