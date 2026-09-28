# syntax=docker/dockerfile:1

# Java 27 images: eclipse-temurin:27 wasn't on Docker Hub yet (Sept 2026), so these stages use
# SapMachine 27, an OpenJDK build that is also a Docker Official Image.
FROM sapmachine:27-jdk-ubuntu AS build
WORKDIR /workspace
# Local-only parent POMs (exist in no remote repo) — seeded from the host ~/.m2 via the "m2" build context
# (all installed versions, so a parent bump needs no edit here). Build from the repo root:
#   docker build --build-context m2=$HOME/.m2/repository -f mcp-server-notification-service/Dockerfile -t mcp-server-notification-service .
COPY --from=m2 com/org/llm/super-pom/ /opt/m2-seed/com/org/llm/super-pom/
COPY --from=m2 com/org/learning/learning-bom/ /opt/m2-seed/com/org/learning/learning-bom/
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY mcp-server-notification-service/pom.xml mcp-server-notification-service/
RUN --mount=type=cache,id=m2repo,target=/root/.m2/repository \
    cp -r /opt/m2-seed/com /root/.m2/repository/ && \
    ./mvnw -B -f mcp-server-notification-service/pom.xml dependency:go-offline
COPY mcp-server-notification-service/src/ mcp-server-notification-service/src/
# git-commit-id-maven-plugin (from super-pom) needs the git metadata
COPY .git/ .git/
RUN --mount=type=cache,id=m2repo,target=/root/.m2/repository \
    cp -r /opt/m2-seed/com /root/.m2/repository/ && \
    ./mvnw -B -f mcp-server-notification-service/pom.xml clean package -DskipTests

FROM sapmachine:27-jre-ubuntu AS extract
WORKDIR /app
COPY --from=build /workspace/mcp-server-notification-service/target/*.jar app.jar
# Boot 3.3+/4 replaced -Djarmode=layertools with the 'tools' jarmode
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM sapmachine:27-jre-ubuntu AS runtime
WORKDIR /app
# The SapMachine image has no curl; the HEALTHCHECK below needs it
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=extract /app/extracted/dependencies/ ./
COPY --from=extract /app/extracted/spring-boot-loader/ ./
COPY --from=extract /app/extracted/snapshot-dependencies/ ./
COPY --from=extract /app/extracted/application/ ./
USER spring:spring
EXPOSE 8083
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1
