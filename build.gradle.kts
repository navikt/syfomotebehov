import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer

group = "no.nav.syfo"

val apacheHttpClientVersion = "5.3.1"
val junitJupiterVersion = "5.10.2"
val kotlinJacksonVersion = "2.16.1"
val flywayVersion = "9.22.3"
val tokenSupportVersion = "3.2.0"
val mockkVersion = "1.13.9"
val springMockkVersion = "3.1.1"
val confluent = "7.6.0"
val isdialogmoteSchema = "1.0.5"
val jsoupVersion = "1.17.2"
val logstashVersion = "4.10"
val logbackVersion = "1.4.11"
val javaxInjectVersion = "1"
val owaspSanitizerVersion = "20211018.2"
val apacheCommonsVersion = "3.14.0"
val jakartaRsApiVersion = "3.1.0"
val hikari = "5.1.0"
val postgres = "42.7.2"
val postgresEmbedded = "0.13.4"

plugins {
    id("java")
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.22"
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
}

allOpen {
    annotation("org.springframework.context.annotation.Configuration")
    annotation("org.springframework.stereotype.Service")
    annotation("org.springframework.stereotype.Component")
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

    implementation("io.micrometer:micrometer-registry-prometheus:1.12.3")
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

    implementation("org.apache.commons:commons-lang3:$apacheCommonsVersion")
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

    constraints {
        implementation("org.apache.zookeeper:zookeeper") {
            because("CVE-2023-44981")
            version {
                require("3.9.1")
            }
        }
    }
}

java.toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    extra["log4j2.version"] = "2.16.0"

    withType<ShadowJar> {
        manifest.attributes["Main-Class"] = "no.nav.syfo.ApplicationKt"
        transform(PropertiesFileTransformer::class.java) {
            paths = listOf("META-INF/spring.factories")
            mergeStrategy = "append"
            isZip64 = true
        }
        configureEach {
            append("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
            append("META-INF/spring/org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration.imports")
        }
        mergeServiceFiles()
        archiveBaseName.set("app")
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    withType<Test> {
        useJUnitPlatform()
    }
}
