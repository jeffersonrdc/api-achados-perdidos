FROM maven:3.9-eclipse-temurin-25 AS base
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src

FROM base AS test
# URL do MySQL efêmero provido pelo CI (via --build-arg). Sem isso, a suíte de
# integração cai no default localhost:3306 e é pulada (assumeTrue) quando não há banco.
ARG TEST_DB_URL
ARG TEST_DB_USER=root
ARG TEST_DB_PASS=
ENV TEST_DB_URL=${TEST_DB_URL} \
    TEST_DB_USER=${TEST_DB_USER} \
    TEST_DB_PASS=${TEST_DB_PASS}
RUN mvn -B test

FROM base AS build
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
ENV SERVER_PORT=8080
COPY --from=build /app/target/app.jar app.jar
RUN mkdir -p arquivos
EXPOSE 8080
# Health check: consulta /actuator/health (exposto restrito, sem detalhes). O wget
# vem do busybox da imagem Alpine. start-period cobre o boot da JVM/Spring.
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD wget -q -O - "http://127.0.0.1:${SERVER_PORT}/actuator/health" | grep -q '"status":"UP"' || exit 1
CMD ["java", "-jar", "app.jar"]
