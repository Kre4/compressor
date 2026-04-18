import java.io.File

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


val jarTask = tasks.named<Jar>("jar")
val jpackageInputDir = layout.buildDirectory.dir("jpackage/input")

val prepareJpackageInput by tasks.registering(Sync::class) {
    group = "distribution"
    description = "Prepare jars for jpackage (app + runtime dependencies)"
    dependsOn(jarTask)
    from(jarTask.map { it.archiveFile })
    from(configurations.runtimeClasspath)
    into(jpackageInputDir)
}

tasks.register<Exec>("packageEncoderExe") {
    group = "distribution"
    description = "Build Windows app-image with encoder.exe"
    dependsOn(prepareJpackageInput)
    doFirst {
        val jarFileName = jarTask.get().archiveFileName.get()
        val destRoot = File(
            layout.buildDirectory.get().asFile,
            "jpackage/encoder-${System.currentTimeMillis()}"
        )
        destRoot.mkdirs()
        commandLine(
            "jpackage",
            "--type", "app-image",
            "--name", "encoder",
            "--input", jpackageInputDir.get().asFile.absolutePath,
            "--main-jar", jarFileName,
            "--main-class", "ru.kre4.compressor.executable.EncoderMainKt",
            "--dest", destRoot.absolutePath,
            "--win-console"
        )
    }
}
tasks.register<Exec>("packageDecoderExe") {
    group = "distribution"
    description = "Build Windows app-image with decoder.exe"
    dependsOn(prepareJpackageInput)
    doFirst {
        val jarFileName = jarTask.get().archiveFileName.get()
        val destRoot = File(
            layout.buildDirectory.get().asFile,
            "jpackage/decoder-${System.currentTimeMillis()}"
        )
        destRoot.mkdirs()
        commandLine(
            "jpackage",
            "--type", "app-image",
            "--name", "decoder",
            "--input", jpackageInputDir.get().asFile.absolutePath,
            "--main-jar", jarFileName,
            "--main-class", "ru.kre4.compressor.executable.DecoderMainKt",
            "--dest", destRoot.absolutePath,
            "--win-console"
        )
    }
}
