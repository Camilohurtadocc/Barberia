FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY applications/app-service/build/libs/servicio-barberos.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
