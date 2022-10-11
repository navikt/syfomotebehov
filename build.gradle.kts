import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.syfo"

object Versions {
    const val apacheHttpClientVersion = "4.5.13"
    const val junitJupiterVersion = "5.8.2"
    const val kotlinJacksonVersion = "2.13.1"
    const val flywayVersion = "8.4.4"
    const val tokenSupportVersion = "1.3.19"
    const val ojdbcVersion = "19.3.0.0"
    const val h2Version = "2.1.210"
    const val mockkVersion = "1.12.7"
    const val springMockkVersion = "3.1.1"
    const val confluent = "7.1.1"
    const val isdialogmoteSchema = "1.0.5"
}

plugins {
    kotlin("jvm") version "1.6.10"
    id("java")
    id("org.jetbrains.kotlin.plugin.allopen") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.springframework.boot") version "2.6.6"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
    }
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
        if (requested.group == "org.scala-lang" && requested.name == "scala-library" && (requested.version == "2.13.6")) {
            useVersion("2.13.9")
            because("fixes critical bug CVE-2022-36944 in 2.13.6")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.kotlinJacksonVersion}")

    implementation("org.apache.httpcomponents:httpclient:${Versions.apacheHttpClientVersion}")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-starter-jersey")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jta-atomikos")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("io.micrometer:micrometer-registry-prometheus:1.8.2")

    implementation("no.nav.security:token-validation-spring:${Versions.tokenSupportVersion}")

    implementation("com.oracle.ojdbc:ojdbc8:${Versions.ojdbcVersion}")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.confluent:kafka-avro-serializer:${Versions.confluent}")
    implementation("io.confluent:kafka-schema-registry:${Versions.confluent}")
    implementation("no.nav.syfo.dialogmote.avro:isdialogmote-schema:${Versions.isdialogmoteSchema}")
    implementation("org.flywaydb:flyway-core:${Versions.flywayVersion}")
    implementation("javax.inject:javax.inject:1")
    implementation("org.slf4j:slf4j-api:1.7.35")
    implementation("net.logstash.logback:logstash-logback-encoder:6.4")
    implementation("org.apache.commons:commons-lang3:3.5")
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20211018.2")

    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.junitJupiterVersion}")
    testImplementation("no.nav.security:token-validation-test-support:${Versions.tokenSupportVersion}")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit")
    }
    testImplementation("com.h2database:h2:${Versions.h2Version}")
    testImplementation("io.mockk:mockk:${Versions.mockkVersion}")
    testImplementation("com.ninja-squad:springmockk:${Versions.springMockkVersion}")
}

tasks {
    extra["log4j2.version"] = "2.16.0"

    withType<Jar> {
        manifest.attributes["Main-Class"] = "no.nav.syfo.ApplicationKt"
    }

    withType<ShadowJar> {
        transform(PropertiesFileTransformer::class.java) {
            paths = listOf("META-INF/spring.factories")
            mergeStrategy = "append"
            isZip64 = true
        }
        mergeServiceFiles()
    }

    named<KotlinCompile>("compileKotlin") {
        kotlinOptions.jvmTarget = "11"
    }

    named<KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = "11"
    }

    test {
        useJUnitPlatform()
    }
}
