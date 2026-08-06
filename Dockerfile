# Package the Maven-built JAR into a small Java runtime image.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/simple-app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
