FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY .mvn .mvn
COPY crmeb-modern-common crmeb-modern-common
COPY crmeb-modern-service crmeb-modern-service
COPY crmeb-modern-admin-api crmeb-modern-admin-api
COPY crmeb-modern-front-api crmeb-modern-front-api
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ARG MODULE
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/${MODULE}/target/*.jar /app/app.jar
EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
