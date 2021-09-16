FROM openjdk:16-alpine3.13
COPY target/*SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-Xmx400M","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar","--spring.profiles.active=docker"]