import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer

group = "no.nav.syfo"

object Versions {
    const val apacheHttpClientVersion = "5.2.1"
    const val junitJupiterVersion = "5.8.2"
    const val kotlinJacksonVersion = "2.13.2"
    const val flywayVersion = "8.4.4"
    const val tokenSupportVersion = "3.1.8"
    const val ojdbcVersion = "19.3.0.0"
    const val h2Version = "2.1.210"
    const val mockkVersion = "1.12.7"
    const val springMockkVersion = "3.1.1"
    const val confluent = "7.5.2"
    const val isdialogmoteSchema = "1.0.5"
    const val jsoupVersion = "1.16.1"
    const val logstashVersion = "4.10"
    const val logbackVersion = "1.4.11"
    const val javaxInjectVersion = "1"
    const val owaspSanitizerVersion = "20211018.2"
    const val apacheCommonsVersion = "3.5"
    const val jakartaRsApiVersion = "3.1.0"
    const val atomikosVersion = "6.0.0"
}

plugins {
    id("java")
    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.0"
    id("org.springframework.boot") version "3.1.5"
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
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.kotlinJacksonVersion}")

    implementation("org.apache.httpcomponents.client5:httpclient5:${Versions.apacheHttpClientVersion}")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jersey")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("io.micrometer:micrometer-registry-prometheus:1.10.5")
    implementation("no.nav.security:token-validation-spring:${Versions.tokenSupportVersion}")
    implementation("com.oracle.ojdbc:ojdbc8:${Versions.ojdbcVersion}")
    implementation("org.springframework.kafka:spring-kafka") {
        exclude(group = "log4j", module = "log4j")
    }

    implementation("net.logstash.logback:logstash-logback-encoder:${Versions.logstashVersion}")
    implementation("ch.qos.logback:logback-classic:${Versions.logbackVersion}")

    implementation("io.confluent:kafka-avro-serializer:${Versions.confluent}")
    implementation("io.confluent:kafka-schema-registry:${Versions.confluent}")
    implementation("no.nav.syfo.dialogmote.avro:isdialogmote-schema:${Versions.isdialogmoteSchema}")
    implementation("org.flywaydb:flyway-core:${Versions.flywayVersion}")
    implementation("javax.inject:javax.inject:${Versions.javaxInjectVersion}")

    implementation("com.atomikos:transactions-spring-boot3-starter:${Versions.atomikosVersion}")

    implementation("org.apache.commons:commons-lang3:${Versions.apacheCommonsVersion}")
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:${Versions.owaspSanitizerVersion}")
    implementation("org.jsoup:jsoup:${Versions.jsoupVersion}")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${Versions.jakartaRsApiVersion}")

    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.junitJupiterVersion}")
    testImplementation("no.nav.security:token-validation-spring-test:${Versions.tokenSupportVersion}")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit")
    }
    testImplementation("com.h2database:h2:${Versions.h2Version}")
    testImplementation("io.mockk:mockk:${Versions.mockkVersion}")
    testImplementation("com.ninja-squad:springmockk:${Versions.springMockkVersion}")
}

java.toolchain {
    languageVersion.set(JavaLanguageVersion.of(19))
    vendor.set(JvmVendorSpec.ADOPTIUM)
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
    }

    withType<Test> {
        useJUnitPlatform()
    }
}
