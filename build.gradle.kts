import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    application
}

group = "com.self"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val exposedVersion = "0.50.1"
val kotlinxSerialization = "1.6.3"
val postgresVersion = "42.7.3"
val cliktVersion = "4.2.0"
val loggingVersion = "7.0.3"
val okHttpVersion = "3.2.0"

dependencies {
    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-dao:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-json:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${exposedVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${kotlinxSerialization}")

    implementation("org.postgresql:postgresql:${postgresVersion}")

    implementation("com.github.ajalt.clikt:clikt:${cliktVersion}")
    implementation("io.github.oshai:kotlin-logging-jvm:$loggingVersion")

    implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("MainKt")
}