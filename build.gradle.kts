import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

val kotlin_version: String by project
val logback_version: String by project
val postgres_version: String by project
val h2_version: String by project
val exposed_version: String by project

plugins {
    kotlin("jvm") version "2.0.20"
    id("io.ktor.plugin") version "2.3.12"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20"
    id("org.flywaydb.flyway") version "9.22.0" // or latest
    id("com.gradleup.shadow") version "8.3.0"
}

group = "com.kartverket"
version = "0.0.1"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.kartverket.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("com.h2database:h2:$h2_version")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.flywaydb:flyway-core:9.16.0")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-swagger")
    implementation("com.azure:azure-identity:1.+")
    implementation("net.minidev:json-smart:2.5.2") /* kan slettes når Azure oppdaterer denne selv*/
    implementation("com.microsoft.graph:microsoft-graph:6.16.0")
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-cio-jvm:2.3.12")
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("org.testcontainers:testcontainers:1.20.5")
    testImplementation("org.apache.commons:commons-compress:1.26.0")
    testImplementation("org.testcontainers:postgresql:1.20.5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
}

flyway {
    url = "jdbc:postgresql://localhost:5432/my_database"
    user = "my_user"
    password = "my_password"
    locations = arrayOf("filesystem:src/main/resources/db/migration")
}

tasks {
    withType<ShadowJar> {
        isZip64 = true
        mergeServiceFiles()
    }
    withType<Test> {
        testLogging {
            showCauses = true
            showExceptions = true
            exceptionFormat = TestExceptionFormat.FULL
            events("passed", "skipped", "failed")
        }
        useJUnitPlatform()
    }
}