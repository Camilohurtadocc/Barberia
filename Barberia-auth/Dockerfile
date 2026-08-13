FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY applications/app-service/build/libs/servicio-auth.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
