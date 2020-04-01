import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.syfo"
version = "1.0.0"

val kotlinLibVersion = "1.3.50"
val kotlinJacksonVersion = "2.9.8"
val flywayVersion = "5.1.4"
val h2Version = "1.4.197"
val oidcSupportVersion = "0.2.18"
val ojdbcVersion = "19.3.0.0"

plugins {
    kotlin("jvm") version "1.3.50"
    id("java")
    id("org.jetbrains.kotlin.plugin.allopen") version "1.3.50"
    id("com.github.johnrengelman.shadow") version "4.0.3"
    id("org.springframework.boot") version "2.1.8.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.0")
    }
}

allOpen {
    annotation("org.springframework.context.annotation.Configuration")
    annotation("org.springframework.stereotype.Service")
    annotation("org.springframework.stereotype.Component")
}

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://repo.adeo.no/repository/maven-releases/")
    maven(url = "https://repo.adeo.no/repository/maven-snapshots/")
    maven(url = "http://packages.confluent.io/maven/")
    maven(url = "https://dl.bintray.com/kotlin/kotlinx/")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinLibVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinLibVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$kotlinJacksonVersion")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-starter-jersey")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jta-atomikos")

    implementation("io.micrometer:micrometer-registry-prometheus:1.0.6")

    implementation("no.nav.security:oidc-spring-support:$oidcSupportVersion")

    implementation("com.oracle.ojdbc:ojdbc8:$ojdbcVersion")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("javax.inject:javax.inject:1")
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("net.logstash.logback:logstash-logback-encoder:4.10")
    implementation("org.apache.commons:commons-lang3:3.5")
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20171016.1")
    implementation("org.projectlombok:lombok:1.16.22")
    annotationProcessor("org.projectlombok:lombok:1.18.6")

    testImplementation("no.nav.security:oidc-test-support:$oidcSupportVersion")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.h2database:h2:$h2Version")
}

tasks {
    withType<Jar> {
        manifest.attributes["Main-Class"] = "no.nav.syfo.Application"
    }

    create("printVersion") {
        doLast {
            println(project.version)
        }
    }

    withType<ShadowJar> {
        transform(PropertiesFileTransformer::class.java) {
            paths = listOf("META-INF/spring.factories")
            mergeStrategy = "append"
        }
        mergeServiceFiles()
    }

    named<KotlinCompile>("compileKotlin") {
        kotlinOptions.jvmTarget = "1.8"
    }

    named<KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = "1.8"
    }
}
