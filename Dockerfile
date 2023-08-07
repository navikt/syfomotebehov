FROM ghcr.io/navikt/baseimages/temurin:19
ENV APPD_ENABLED=true
LABEL org.opencontainers.image.source=https://github.com/navikt/syfomotebehov

COPY init.sh /init-scripts/init.sh

COPY build/libs/*.jar app.jar

ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"
