plugins {
    id("com.github.johnrengelman.shadow") version "6.0.0"
    java
    `maven-publish`
    `java-library`
}

group = "de.eldoria"
version = "2.4.1b"
var mainPackage = "bigdoorsopener"
val shadebade = group as String? + "." + mainPackage + "."
val name = "BigDoorsOpener"
description = "Open and close doors automatically on certain conditions"

val lombokVersion = "1.18.20"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.maven.apache.org/maven2/") }
    maven { url = uri("https://eldonexus.de/repository/maven-releases/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://repo.codemc.org/repository/maven-public") }
    maven { url = uri("https://maven.enginehub.org/repo/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
    maven { url = uri("https://mvn.lumine.io/repository/maven-public/") }
    maven { url = uri("https://repo.maven.apache.org/maven2/") }
}

dependencies {
    implementation("de.eldoria", "eldo-util", "1.8.4")
    implementation("net.kyori", "adventure-api", "4.7.0")
    implementation("net.kyori", "adventure-platform-bukkit", "4.0.0-SNAPSHOT")
    compileOnly("org.spigotmc", "spigot-api", "1.13.2-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains", "annotations", "20.1.0")
    compileOnly("com.sk89q.worldguard", "worldguard-bukkit", "7.0.5-SNAPSHOT") {
        exclude("org.spigotmc", "spigot-api")
    }
    compileOnly("me.clip", "placeholderapi", "2.10.6")
    compileOnly("io.lumine.xikage", "MythicMobs", "4.9.1")
    compileOnly(files("../dependency/BigDoors.jar"))
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.5.2")
    compileOnly("org.projectlombok", "lombok", lombokVersion)
    annotationProcessor("org.projectlombok", "lombok", lombokVersion)
    testCompileOnly("org.projectlombok", "lombok", lombokVersion)
    testAnnotationProcessor("org.projectlombok", "lombok", lombokVersion)
}


java {
    withSourcesJar()
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifact(tasks["shadowJar"])
        artifact(tasks["sourcesJar"])
        artifact(tasks["javadocJar"])
        groupId = project.group as String?
        artifactId = project.name
        version = project.version as String?
    }
}

tasks {
    processResources {
        from(sourceSets.main.get().resources.srcDirs) {
            filesMatching("plugin.yml") {
                expand(
                        "name" to project.name,
                        "version" to project.version,
                        "description" to project.description
                )
            }
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    shadowJar {
        relocate("de.eldoria.eldoutilities", shadebade + "eldoutilities")
        relocate("net.kyori", shadebade + "kyori")
        mergeServiceFiles()
        archiveClassifier.set("")
    }

    test {
        useJUnit()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}