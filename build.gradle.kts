plugins {
    java
    `maven-publish`
    alias(libs.plugins.spotless)
    alias(libs.plugins.publishdata)
    alias(libs.plugins.shadow)
    alias(libs.plugins.runserver)
}

description = "Open and close doors automatically on certain conditions"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://eldonexus.de/repository/maven-public/")
    maven("https://eldonexus.de/repository/maven-proxies/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    implementation(libs.eldoutil.legacy)
    implementation(libs.adventure.api)
    implementation(libs.adventure.platform.bukkit)
    compileOnly(libs.spigot)
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.worldguard.bukkit) {
        exclude("org.spigotmc", "spigot-api")
        exclude("com.sk89q.worldedit.worldedit-libs", "core")
    }
    compileOnly(libs.placeholderapi)
    compileOnly(libs.bigdoors)
    compileOnly(libs.mythic.dist)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
    withJavadocJar()
}

spotless {
    java {
        licenseHeaderFile(rootProject.file("HEADER.txt"))
        target("**/*.java")
    }
}

publishData {
    eldoNexus(dev = false)
    component = "shadow"
    buildData {
        put("lynaProduct", "3")
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            url.set("https://github.com/eldoriarpg/BigDoorOpener")
            licenses {
                license {
                    name.set("GNU Affero General Public License v3.0")
                    url.set("https://github.com/eldoriarpg/BigDoorOpener/blob/master/LICENSE.md")
                }
            }
        }
    }
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand(
                "name" to "BigDoorsOpener",
                "version" to project.version.toString(),
                "description" to project.description!!
            )
        }
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    compileTestJava {
        options.encoding = "UTF-8"
    }

    test {
        dependsOn(spotlessCheck)
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    shadowJar {
        val shadebase = "de.eldoria.bigdoorsopener.libs."
        relocate("de.eldoria.eldoutilities", shadebase + "utilities")
        relocate("net.kyori", shadebase + "kyori")
        mergeServiceFiles()
        addMultiReleaseAttribute = false
        exclude("META-INF/versions/**")
        archiveBaseName.set("BigDoorsOpener")
    }

    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("1.21.11")
        jvmArgs("-Dcom.mojang.eula.agree=true")
    }
}
