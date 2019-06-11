FROM openjdk:11.0.3-slim-stretch
COPY maven/${app.artifactId}-${project.version}.jar app.jar
# Turns on JMX to expose metrics
# TODO - Need to automatically set java.rmi.server.hostname (See LORE-112 and LORE-144)
ENTRYPOINT ["java",\
    "-Dserver.port=9090",\
    "-Dspring.profiles.active=int",\
    "-Djava.security.egd=file:/dev/./urandom",\
    "-Dcom.sun.management.jmxremote=true",\
    "-Dcom.sun.management.jmxremote.local.only=false",\
    "-Dcom.sun.management.jmxremote.authenticate=false",\
    "-Dcom.sun.management.jmxremote.ssl=false",\
    "-Djava.rmi.server.hostname=transform-api",\
    "-Dcom.sun.management.jmxremote.port=9999",\
    "-Dcom.sun.management.jmxremote.rmi.port=9998",\
    "-jar",\
    "/app.jar"]
EXPOSE 9090 9998 9999