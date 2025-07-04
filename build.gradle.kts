import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

val kotlin_version: String by project
val ktor_version: String by project
val logback_version: String by project
val postgres_version: String by project
val h2_version: String by project
val exposed_version: String by project
val testcontainers_version = "1.21.3"
val mockk_version = "1.14.3"
val flyway_version = "11.10.1"
val microsoft_graph_version = "6.42.1"

plugins {
    kotlin("jvm") version "2.1.21"
    id("io.ktor.plugin") version "3.1.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.21"
    id("org.flywaydb.flyway") version "11.10.0"
    id("com.gradleup.shadow") version "8.3.6"
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
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("com.h2database:h2:$h2_version")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.flywaydb:flyway-core:$flyway_version")
    implementation("org.flywaydb:flyway-database-postgresql:$flyway_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-swagger:$ktor_version")
    implementation("com.azure:azure-identity:1.+")
    implementation("net.minidev:json-smart:2.5.2") /* kan slettes når Azure oppdaterer denne selv*/
    implementation("com.microsoft.graph:microsoft-graph:$microsoft_graph_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-cio-jvm:$ktor_version")
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
    testImplementation("io.mockk:mockk:$mockk_version")
    testImplementation("org.testcontainers:testcontainers:$testcontainers_version")
    testImplementation("org.testcontainers:postgresql:$testcontainers_version")
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
