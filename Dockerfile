FROM ghcr.io/distroless/java21

ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75  -Dspring.profiles.active=remote"
ENV TZ="Europe/Oslo"

COPY build/libs/app.jar app.jar
