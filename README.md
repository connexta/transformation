# Transformation Service Implementation

## Building
### What you need ###
* [Install Java SE 11](https://jdk.java.net/java-se-ri/11).
* Make sure that your JAVA\_HOME environment variable is set to the newly installed JDK location, and that your PATH includes %JAVA\_HOME%\bin (Windows) or $JAVA\_HOME$/bin (\*NIX).
* [Install Maven 3.5.2 \(or later\)](http://maven.apache.org/download.html). Make sure that your PATH includes the MVN\_HOME/bin directory.
* Set the MAVEN_OPTS variable with appropriate memory settings.

### How to build ###
Clone the ION Transformation code repository.

```
git clone git@github.com:connexta/ion-transformation.git
```

Change to the root directory of the cloned ion-transformation repository. Run the following command:

```
mvn clean install
```

## Running in Development

```
cd <ion-transformation root>/distros/spring
```

```
Set the following environment variables for the Lookup Service Endpoint:
${PROTOCOL}://${LOOKUP_SERVICE_HOST}:${LOOKUP_SERVICE_PORT}/lookup
```

```
java "-Dserver.port=8080" "-Dspring.profiles.active=dev" -jar .\target\transformation-distros-spring-<version>.jar
```

## Push a Docker image to a registry

Prerequisite: If using an insecure Docker registry, update ~/.docker/daemon.json to include the appropriate
              insecure-registries and restart Docker.

Change to the root directory of the cloned ion-transformation repository. Run the following command:

```
mvn clean install "-Ddocker.push.registry=myPushRegistry" docker:push
```

## Formatting
If during development the build fails for formatting violations:

You can check for formatting violations with the command:

```
mvn initialize spotless:check
```

You can fix formatting violations with the command:

```
mvn initialize spotless:apply
```