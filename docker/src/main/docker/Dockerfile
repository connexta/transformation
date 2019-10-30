FROM openjdk:11.0.3-slim-stretch
COPY maven/${app.artifactId}-${project.version}.jar app.jar
ENTRYPOINT ["java",\
    "-Dserver.port=9090",\
    "-Dspring.profiles.active=int",\
    "-Djava.security.egd=file:/dev/./urandom",\
    "-jar",\
    "/app.jar"]
EXPOSE 9090