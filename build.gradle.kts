plugins {
    kotlin("jvm") version "2.1.10"
    application
}

group = "ru.kre4"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "ru.kre4.compressor.EncoderMainKt"
}

tasks.register<JavaExec>("runEncoder") {
    group = "application"
    description = "Run encoder infile zipfile"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.kre4.compressor.executable.EncoderMainKt")
    args((project.findProperty("appArgs") as String?)?.split(" ") ?: emptyList<String>())
}

tasks.register<JavaExec>("runDecoder") {
    group = "application"
    description = "Run decoder zipfile decfile"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.kre4.compressor.executable.DecoderMainKt")
    args((project.findProperty("appArgs") as String?)?.split(" ") ?: emptyList<String>())
}

tasks.register<JavaExec>("runReport") {
    group = "application"
    description = "Run compression report on dataset"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.kre4.compressor.executable.ReportMainKt")
}
