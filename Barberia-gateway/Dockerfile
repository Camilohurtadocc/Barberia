FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY applications/app-service/build/libs/servicio-gateway.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
