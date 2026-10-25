# Runtime image only. Build the jar first: mvn -B verify
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home storeops
COPY target/storeops-api-0.1.0-SNAPSHOT.jar app.jar
USER storeops
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
