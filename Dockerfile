FROM gradle:8.8 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon -x test
ENV env=development
FROM eclipse-temurin:21
EXPOSE 8080:8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/backend.jar

ENTRYPOINT ["java", "-jar", "/app/backend.jar"]