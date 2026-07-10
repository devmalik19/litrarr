FROM eclipse-temurin:25-jdk-alpine
COPY target/*.jar litrarr.jar
EXPOSE 8021
ENTRYPOINT ["java", "-jar", "/litrarr.jar"]