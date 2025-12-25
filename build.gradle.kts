group = "no.nav.syfo"

val junitJupiterVersion = "5.13.4"
val kotlinJacksonVersion = "2.20.1"
val flywayVersion = "11.19.1"
val tokenSupportVersion = "3.2.0"
val mockkVersion = "1.14.7"
val nimbusVersion = "9.37.4"
val kotestTestContainersExtensionVersion = "2.0.2"
val wiremockKotestExtensionVersion = "3.1.0"
val springMockkVersion = "4.0.2"
val confluent = "7.9.0"
val isdialogmoteSchema = "1.0.5"
val jsoupVersion = "1.21.2"
val logstashVersion = "4.10"
val javaxInjectVersion = "1"
val owaspSanitizerVersion = "20240325.1"
val apacheCommonsTextVersion = "1.15.0"
val apacheMinaVersion = "2.2.5"
val jakartaRsApiVersion = "4.0.0"
val hikari = "5.1.0"
val postgres = "42.7.8"
val detektVersion = "1.23.8"
val testcontainersVersion = "1.21.3"
val kotestVersion = "5.9.1"
val springKotestExtensionVersion = "1.3.0"

plugins {
    id("java")
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
}

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.scala-lang" &&
            requested.name == "scala-library" &&
            requested.version == "2.13.6"
        ) {
            useVersion("2.13.9")
            because("fixes critical bug CVE-2022-36944 in 2.13.6")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$kotlinJacksonVersion")

    implementation("org.apache.httpcomponents.client5:httpclient5")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jersey")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    implementation("org.springframework.kafka:spring-kafka") {
        exclude(group = "log4j", module = "log4j")
    }

    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("ch.qos.logback:logback-classic")

    implementation("io.confluent:kafka-avro-serializer:$confluent")
    implementation("io.confluent:kafka-schema-registry:$confluent")
    implementation("no.nav.syfo.dialogmote.avro:isdialogmote-schema:$isdialogmoteSchema")
    implementation("javax.inject:javax.inject:$javaxInjectVersion")

    implementation("org.apache.commons:commons-text:$apacheCommonsTextVersion")
    implementation("org.apache.mina:mina-core:$apacheMinaVersion")
    implementation("com.nimbusds:nimbus-jose-jwt:$nimbusVersion")
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:$owaspSanitizerVersion")
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:$jakartaRsApiVersion")

    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikari")
    implementation("org.postgresql:postgresql:$postgres")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit")
    }
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")

    testImplementation("io.kotest:kotest-framework-datatest:5.9.1")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:$springKotestExtensionVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.ninja-squad:springmockk:$springMockkVersion")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:$kotestTestContainersExtensionVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-wiremock:$wiremockKotestExtensionVersion")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
}

java.toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    extra["log4j2.version"] = "2.16.0"

    named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
        this.archiveFileName.set("app.jar")
    }

    withType<Test> {
        useJUnitPlatform()
    }
}

detekt {
    config.from("detekt-config.yml")
    buildUponDefaultConfig = true
    baseline = file("detekt-baseline.xml")
}
