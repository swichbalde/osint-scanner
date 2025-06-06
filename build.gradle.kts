import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}

group = "com.self"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val exposedVersion = "0.50.1"
val kotlinxSerialization = "1.6.3"
val kotlinxCoroutinesVersion = "1.9.0"
val postgresVersion = "42.7.3"
val cliktVersion = "4.2.0"
val loggingVersion = "7.0.3"
val slf4jVersion = "2.0.13"
val okHttpVersion = "4.12.0"
val poiXmlVersion = "5.2.5"
val mokkVersion = "1.14.2"
val mockWebServerVersion = "4.9.3"

dependencies {
    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-dao:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-json:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${exposedVersion}")
    implementation("org.postgresql:postgresql:${postgresVersion}")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${kotlinxSerialization}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")

    implementation("com.github.ajalt.clikt:clikt:${cliktVersion}")

    implementation("io.github.oshai:kotlin-logging-jvm:$loggingVersion")
    implementation("org.slf4j:slf4j-simple:${slf4jVersion}")

    implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")

    implementation("org.apache.poi:poi-ooxml:$poiXmlVersion")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:$mokkVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:$mockWebServerVersion")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}

application {
    mainClass.set("MainKt")
}