FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY inventory-service/pom.xml /workspace/inventory-service/pom.xml
COPY order-service/pom.xml /workspace/order-service/pom.xml
COPY gateway-service/pom.xml /workspace/gateway-service/pom.xml
RUN mvn -q -N install
COPY gateway-service/src /workspace/gateway-service/src
WORKDIR /workspace/gateway-service
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/gateway-service/target/gateway-service.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
