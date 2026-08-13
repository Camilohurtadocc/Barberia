FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY applications/app-service/build/libs/servicio-citas.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
