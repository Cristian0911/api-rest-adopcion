# syntax=docker/dockerfile:1

# Etapa 1: compilación con Maven y JDK 21.
# Las pruebas se omiten aquí (-DskipTests) para no necesitar base de datos ni Docker durante la
# construcción; se ejecutan por separado con ./mvnw verify y ./mvnw verify -Ppostgres.
FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package \
    && cp target/adopcion-*.jar /build/app.jar

# Etapa 2: ejecución con JRE 21 y un usuario sin privilegios.
FROM eclipse-temurin:21-jre-noble
WORKDIR /app

RUN groupadd --system --gid 10001 adopcion \
    && useradd --system --uid 10001 --gid adopcion --no-create-home --shell /usr/sbin/nologin adopcion

# El JAR queda como propiedad de root: el usuario de ejecución puede leerlo pero no modificarlo
COPY --from=build /build/app.jar /app/app.jar

USER adopcion
EXPOSE 8080

# El perfil se elige con SPRING_PROFILES_ACTIVE y la configuración llega por variables de entorno;
# la imagen no contiene credenciales ni archivos .env.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
