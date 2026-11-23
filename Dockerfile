FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
#RUN mvn clean package -DskipTests
RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Crear el directorio para el certificado SSL
RUN mkdir -p /app/certs
# El certificado debe estar en src/main/resources para ser copiado
COPY src/main/resources/prod-ca-2021.crt /app/certs/prod-ca-2021.crt

ENTRYPOINT ["java", "-jar", "app.jar"]