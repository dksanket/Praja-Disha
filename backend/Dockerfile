# Runtime-only image. The executable JAR is built by CI (mvn package) and
# copied into the build context as app.jar before `docker compose build`.
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Uploaded media (photos / videos / voice) is written here; mounted as a
# named volume in docker-compose so it survives container rebuilds.
RUN mkdir -p /app/uploads

COPY app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
