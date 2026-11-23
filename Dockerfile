FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25
WORKDIR /app
COPY build/libs/app.jar app.jar
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75 -Dspring.profiles.active=remote"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER 65532
ENTRYPOINT ["java", "-jar", "app.jar"]
