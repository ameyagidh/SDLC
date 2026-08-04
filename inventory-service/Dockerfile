FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY inventory-service/pom.xml /workspace/inventory-service/pom.xml
COPY order-service/pom.xml /workspace/order-service/pom.xml
COPY gateway-service/pom.xml /workspace/gateway-service/pom.xml
RUN mvn -q -N install
COPY inventory-service/src /workspace/inventory-service/src
WORKDIR /workspace/inventory-service
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/inventory-service/target/inventory-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
