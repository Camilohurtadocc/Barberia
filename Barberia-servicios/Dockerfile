FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY applications/app-service/build/libs/servicio-catalogo.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
