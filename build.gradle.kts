import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP

plugins {
    java
    `maven-publish`
    alias(libs.plugins.spotless)
    alias(libs.plugins.publishdata)
    alias(libs.plugins.shadow)
    alias(libs.plugins.pluginyml.bukkit)
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
    compileOnly(libs.paper)
    implementation(libs.bundles.utilities)
    implementation(platform(libs.jackson.bom))
    implementation(libs.bundles.jackson)
    implementation(libs.bstats)
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.worldguard.bukkit) {
        exclude("org.spigotmc", "spigot-api")
        exclude("com.sk89q.worldedit.worldedit-libs", "core")
        exclude("net.kyori")
    }
    compileOnly(libs.placeholderapi)
    compileOnly(libs.bigdoors)
    compileOnly(libs.mythic.dist)

    testImplementation(libs.paper)
    testImplementation(libs.bigdoors)
    testImplementation(libs.mythic.dist)
    testImplementation(libs.placeholderapi)
    testImplementation(libs.worldguard.bukkit) {
        exclude("org.spigotmc", "spigot-api")
        exclude("com.sk89q.worldedit.worldedit-libs", "core")
        exclude("net.kyori")
    }
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
        relocate("org.bstats", shadebase + "bstats")
        relocate("com.fasterxml", shadebase + "fasterxml")
        relocate("tools.jackson", shadebase + "jackson")
        relocate("org.snakeyaml", shadebase + "snakeyaml")
        relocate("dev.chojo.ocular", shadebase + "ocular")
        relocate("de.eldoria.jacksonbukkit", shadebase + "jacksonbukkit")
        relocate("de.eldoria.eldoutilities", shadebase + "utilities")
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

val conditionGroups = listOf("item", "location", "worldLocation", "permission", "time", "weather", "placeholder", "mythicMob")

bukkit {
    name = "BigDoorsOpener"
    main = "de.eldoria.bigdoorsopener.core.BigDoorsOpener"
    version = project.version.toString()
    description = project.description
    apiVersion = "1.21.11"
    authors = listOf("RainbowDashLabs")
    website = "https://github.com/eldoriarpg/BigDoorOpener"
    depend = listOf("BigDoors")
    softDepend = listOf("WorldGuard", "PlaceholderAPI", "MythicMobs", "NashornJs")

    commands {
        register("bigdoorsopener") {
            description = "Manage conditional doors"
            aliases = listOf("bdo")
        }
    }

    permissions {
        register("bdo.*") {
            description = "All Big Doors Opener permissions"
            default = OP
            children = listOf(
                "bdo.command.use", "bdo.command.givekey", "bdo.command.reload", "bdo.customEvaluator",
                "bdo.accessAll", "bdo.condition.all", "bdo.updates"
            )
        }
        register("bdo.command.use") {
            description = "Use the door commands"
            default = OP
        }
        register("bdo.command.givekey") {
            description = "Give keys of item conditions"
            default = OP
        }
        register("bdo.command.reload") {
            description = "Reload the plugin and create debug reports"
            default = OP
        }
        register("bdo.customEvaluator") {
            description = "Set custom evaluators"
            default = OP
        }
        register("bdo.accessAll") {
            description = "Manage doors of other players"
            default = OP
        }
        register("bdo.condition.all") {
            description = "Use all conditions"
            default = OP
            children = conditionGroups.map { "bdo.condition.$it" }
        }
        conditionGroups.forEach { group ->
            register("bdo.condition.$group") {
                description = "Use $group conditions"
                default = OP
            }
        }
        register("bdo.updates") {
            description = "Hear about new versions on join and download them"
            default = OP
        }
    }
}
