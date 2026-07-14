# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
# Local-only parent POMs (exist in no remote repo) — seeded from the host ~/.m2 via the "m2" build context
COPY --from=m2 com/org/llm/super-pom/1.0.0/ /opt/m2-seed/com/org/llm/super-pom/1.0.0/
COPY --from=m2 com/org/learning/learning-bom/1.1.0/ /opt/m2-seed/com/org/learning/learning-bom/1.1.0/
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY mcp-server-deployment-service/pom.xml mcp-server-deployment-service/
RUN --mount=type=cache,id=m2repo,target=/root/.m2/repository \
    cp -r /opt/m2-seed/com /root/.m2/repository/ && \
    ./mvnw -B -f mcp-server-deployment-service/pom.xml dependency:go-offline
COPY mcp-server-deployment-service/src/ mcp-server-deployment-service/src/
# git-commit-id-maven-plugin (from super-pom) needs the git metadata
COPY .git/ .git/
RUN --mount=type=cache,id=m2repo,target=/root/.m2/repository \
    cp -r /opt/m2-seed/com /root/.m2/repository/ && \
    ./mvnw -B -f mcp-server-deployment-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:25-jre AS extract
WORKDIR /app
COPY --from=build /workspace/mcp-server-deployment-service/target/*.jar app.jar
# Boot 3.3+/4 replaced -Djarmode=layertools with the 'tools' jarmode
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=extract /app/extracted/dependencies/ ./
COPY --from=extract /app/extracted/spring-boot-loader/ ./
COPY --from=extract /app/extracted/snapshot-dependencies/ ./
COPY --from=extract /app/extracted/application/ ./
USER spring:spring
EXPOSE 8082
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1
