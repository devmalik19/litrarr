FROM eclipse-temurin:25-jdk-alpine
# 7-Zip converts RAR5 and 7z comics to CBZ
RUN apk add --no-cache 7zip
COPY target/*.jar litrarr.jar
EXPOSE 3800
ENTRYPOINT ["java", "-jar", "/litrarr.jar"]
