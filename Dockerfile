FROM eclipse-temurin:25-jdk-alpine
COPY target/*.jar litrarr.jar
EXPOSE 3800
ENTRYPOINT ["java", "-jar", "/litrarr.jar"]