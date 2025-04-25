FROM gradle:8.13 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon

# Use a minimal JDK image to run the application
FROM eclipse-temurin:21-jre-alpine
RUN apk upgrade --no-cache
# Create a non-root
RUN mkdir /app && adduser -D user && chown -R user /app
EXPOSE 8080
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/frisk-backend.jar
USER user

ENTRYPOINT ["java","-jar","/app/frisk-backend.jar"]
