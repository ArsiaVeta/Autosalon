  FROM eclipse-temurin:17-jre-alpine
  WORKDIR /app
  COPY StorageService/build/libs/*.jar app.jar
  EXPOSE 8080 9090
  ENTRYPOINT ["java", "-jar", "/app/app.jar"]