FROM gradle:8.14.3 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon

# Use a minimal JDK image to run the application
FROM eclipse-temurin:24.0.2_12-jre-alpine-3.22
RUN apk upgrade --no-cache
# Create a non-root
RUN mkdir /app && adduser -D user && chown -R user /app
EXPOSE 8080
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/frisk-backend.jar
USER user

ENTRYPOINT ["java","-jar","/app/frisk-backend.jar"]
