FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml maven-settings.xml ./
COPY src/ src/
RUN mvn -B -ntp --settings maven-settings.xml package org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies -DincludeScope=runtime

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/target/sirkula-server.jar /app/sirkula-server.jar
COPY --from=build /src/target/dependency /app/lib
ENV SIRKULA_DB=/var/data/sirkula.db
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/app/sirkula-server.jar:/app/lib/*", "sirkula.ServerKt"]
