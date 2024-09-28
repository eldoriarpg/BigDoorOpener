plugins {
    id("org.cadixdev.licenser") version "0.6.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    java
    `maven-publish`
    `java-library`
    id("de.chojo.publishdata") version "1.4.0"
}

group = "de.eldoria"
version = "2.5.3"
var mainPackage = "bigdoorsopener"
val shadebade = group as String? + "." + mainPackage + "."
val name = "BigDoorsOpener"
description = "Open and close doors automatically on certain conditions"

repositories {
    maven("https://eldonexus.de/repository/maven-public/")
    maven("https://eldonexus.de/repository/maven-proxies/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    implementation("de.eldoria", "eldo-util", "1.14.5")
    implementation("net.kyori", "adventure-api", "4.17.0")
    implementation("net.kyori", "adventure-platform-bukkit", "4.3.4")
    compileOnly("org.spigotmc", "spigot-api", "1.13.2-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains", "annotations", "25.0.0")
    compileOnly("com.sk89q.worldguard", "worldguard-bukkit", "7.0.9") {
        exclude("org.spigotmc", "spigot-api")
        exclude("com.sk89q.worldedit.worldedit-libs", "core")
    }

    compileOnly("me.clip", "placeholderapi", "2.11.6")
    compileOnly("nl.pim16aap2", "BigDoors", "0.1.8.46")
    compileOnly("io.lumine", "Mythic-Dist", "5.7.2")
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.11.1")
}

license {
    header(rootProject.file("HEADER.txt"))
    include("**/*.java")
}

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

publishData {
    addBuildData()
    useEldoNexusRepos()
    publishComponent("java")
}

publishing {
    publications.create<MavenPublication>("maven") {
        publishData.configurePublication(this)
    }

    repositories {
        maven {
            authentication {
                credentials(PasswordCredentials::class) {
                    username = System.getenv("NEXUS_USERNAME")
                    password = System.getenv("NEXUS_PASSWORD")
                }
            }

            name = "EldoNexus"
            url = uri(publishData.getRepository())
        }
    }
}

tasks {
    processResources {
        from(sourceSets.main.get().resources.srcDirs) {
            filesMatching("plugin.yml") {
                expand(
                    "name" to "BigDoorsOpener",
                    "version" to publishData.getVersion(true),
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
        minimize()
        archiveClassifier.set("all")
        archiveBaseName.set("BigDoorsOpener")
    }

    test {
        useJUnit()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    register<Copy>("copyToServer") {
        val path = project.property("targetDir") ?: ""
        if (path.toString().isEmpty()) {
            println("targetDir is not set in gradle properties")
            return@register
        }
        from(shadowJar)
        destinationDir = File(path.toString())
    }

    build {
        dependsOn(shadowJar)
    }
}
