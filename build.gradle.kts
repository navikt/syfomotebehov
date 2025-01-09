group = "no.nav.syfo"

val apacheHttpClientVersion = "5.3.1"
val junitJupiterVersion = "5.10.3"
val kotlinJacksonVersion = "2.17.2"
val flywayVersion = "9.22.3"
val tokenSupportVersion = "3.2.0"
val mockkVersion = "1.13.12"
val springMockkVersion = "4.0.2"
val confluent = "7.7.0"
val isdialogmoteSchema = "1.0.5"
val jsoupVersion = "1.18.1"
val logstashVersion = "4.10"
val logbackVersion = "1.5.6"
val javaxInjectVersion = "1"
val owaspSanitizerVersion = "20240325.1"
val apacheCommonsTextVersion = "1.12.0"
val jakartaRsApiVersion = "4.0.0"
val hikari = "5.1.0"
val postgres = "42.7.3"
val postgresEmbedded = "1.1.0"
val detektVersion = "1.23.6"

plugins {
    id("java")
    id("org.springframework.boot") version "3.3.6"
    id("io.spring.dependency-management") version "1.1.6"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
}

val githubUser: String by project
val githubPassword: String by project

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
    maven(url = "https://jitpack.io")
    maven {
        url = uri("https://maven.pkg.github.com/navikt/isdialogmote-schema")
        credentials {
            username = githubUser
            password = githubPassword
        }
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

    implementation("org.apache.httpcomponents.client5:httpclient5:$apacheHttpClientVersion")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jersey")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("io.micrometer:micrometer-registry-prometheus:1.12.8")
    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    implementation("org.springframework.kafka:spring-kafka") {
        exclude(group = "log4j", module = "log4j")
    }

    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("io.confluent:kafka-avro-serializer:$confluent")
    implementation("io.confluent:kafka-schema-registry:$confluent")
    implementation("no.nav.syfo.dialogmote.avro:isdialogmote-schema:$isdialogmoteSchema")
    implementation("javax.inject:javax.inject:$javaxInjectVersion")

    implementation("org.apache.commons:commons-text:$apacheCommonsTextVersion")
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:$owaspSanitizerVersion")
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:$jakartaRsApiVersion")

    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikari")
    implementation("org.postgresql:postgresql:$postgres")
    testImplementation("com.opentable.components:otj-pg-embedded:$postgresEmbedded")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit")
    }
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.ninja-squad:springmockk:$springMockkVersion")

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

