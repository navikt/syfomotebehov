import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.syfo"
version = "1.0.0"

val springBootVersion = "2.0.4.RELEASE"
val oidcSupportVersion = "0.2.7"
val kotlinLibVersion = "1.3.50"
val kotlinJacksonVersion = "2.9.8"

plugins {
    kotlin("jvm") version "1.3.50"
    id("java")
    id("org.jetbrains.kotlin.plugin.allopen") version "1.3.50"
    id("com.github.johnrengelman.shadow") version "4.0.3"
    id("org.springframework.boot") version "2.0.4.RELEASE"
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

    implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-logging:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-jersey:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-cache:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-jta-atomikos:$springBootVersion")

    implementation("io.micrometer:micrometer-registry-prometheus:1.0.6")

    implementation("no.nav.security:oidc-spring-support:$oidcSupportVersion")
    implementation("no.nav.security:oidc-support:$oidcSupportVersion")

    implementation("com.oracle:ojdbc8:12.2.0.1")
    implementation("org.springframework.kafka:spring-kafka:2.1.8.RELEASE")
    implementation("org.flywaydb:flyway-core:5.0.7")
    implementation("org.bitbucket.b_c:jose4j:0.5.0")
    implementation("javax.inject:javax.inject:1")
    implementation("javax.ws.rs:javax.ws.rs-api:2.0.1")
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("net.logstash.logback:logstash-logback-encoder:4.10")
    implementation("org.apache.commons:commons-lang3:3.5")
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20171016.1")
    implementation("org.projectlombok:lombok:1.16.22")
    annotationProcessor("org.projectlombok:lombok:1.18.6")

    testImplementation("no.nav.security:oidc-spring-test:0.2.4")
    testImplementation("org.springframework.kafka:spring-kafka-test:2.1.8.RELEASE")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation("com.h2database:h2:1.4.197")
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
