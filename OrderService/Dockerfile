  FROM eclipse-temurin:17-jre-alpine
  WORKDIR /app
  COPY OrderService/build/libs/*.jar app.jar
  EXPOSE 8080
  ENTRYPOINT ["java", "-jar", "/app/app.jar"]
