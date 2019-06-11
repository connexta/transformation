# Transformation Service Implementation

## Building
### What you need ###
* [Install Java SE 11](https://jdk.java.net/java-se-ri/11).
* Make sure that your JAVA\_HOME environment variable is set to the newly installed JDK location, and that your PATH includes %JAVA\_HOME%\bin (Windows) or $JAVA\_HOME$/bin (\*NIX).
* [Install Maven 3.5.2 \(or later\)](http://maven.apache.org/download.html). Make sure that your PATH includes the MVN\_HOME/bin directory.
* Set the MAVEN_OPTS variable with appropriate memory settings.

### How to build ###
Clone the Transformation code repository.

```
git clone git@github.com:connexta/transformation.git
```

Change to the root directory of the cloned transformation repository. Run the following command:

```
mvn clean install
```

## Running in Development

```
cd <transformation root>/distros/spring
```

```
java "-Dserver.port=8080" "-Dspring.profiles.active=dev" -jar .\target\transformation-distros-spring-<version>.jar
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