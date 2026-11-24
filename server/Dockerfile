FROM gradle:9.5.0-jdk17 AS build
WORKDIR /src
COPY . .
RUN gradle --no-daemon installDist

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/build/install/sirkula-server /app
ENV SIRKULA_DB=/var/data/sirkula.db
EXPOSE 8080
ENTRYPOINT ["/app/bin/sirkula-server"]
