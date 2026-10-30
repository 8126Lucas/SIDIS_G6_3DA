FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["java","-Xmx256m","-jar","/app.jar"]