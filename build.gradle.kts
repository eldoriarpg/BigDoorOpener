import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP

plugins {
    java
    `maven-publish`
    alias(libs.plugins.spotless)
    alias(libs.plugins.publishdata)
    alias(libs.plugins.publishdata.manifest)
    alias(libs.plugins.shadow)
    alias(libs.plugins.pluginyml.bukkit)
    alias(libs.plugins.runserver)
    alias(libs.plugins.minotaur)
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
    testImplementation(libs.junit.jupiter.params)
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
        target("src/**/*.java")
        licenseHeaderFile(rootProject.file("HEADER.txt"))
        trimTrailingWhitespace()
        endWithNewline()
        palantirJavaFormat(libs.versions.palantir.get()).formatJavadoc(false)
        removeUnusedImports()
        importOrder("", "java", "javax", "\\#")
        encoding("UTF-8")
    }
}

publishData {
    eldoNexus(dev = false)
    component = "shadow"
    buildData {
        put("lynaProduct", "3")
    }
}

pluginManifest {
    platforms("paper")
    requires("BigDoors", min = "0.1.8") { modrinth("bigdoors") }
    optional("WorldGuard", min = "7.0") { modrinth("worldguard") }
    optional("PlaceholderAPI") { modrinth("placeholderapi") }
    optional("MythicMobs", min = "5.0") { modrinth("mythicmobs") }
    optional("NashornJs") { spigot(91204) }
}

modrinth {
    token = providers.environmentVariable("MODRINTH_TOKEN")
    projectId = "2mTWavvv"
    versionNumber = project.version.toString()
    versionType = "release"
    uploadFile.set(tasks.shadowJar)
    gameVersions.addAll("1.21.11", "26.1", "26.1.1", "26.1.2", "26.2", "26.3")
    loaders.addAll("paper", "purpur")
    dependencies {
        required.project("bigdoors")
        optional.project("worldguard")
        optional.project("placeholderapi")
        optional.project("mythicmobs")
    }
    changelog = providers.environmentVariable("MODRINTH_CHANGELOG").orElse("")
    syncBodyFrom = rootProject.file("readme.md").readText()
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
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
        addMultiReleaseAttribute = false
        exclude("META-INF/versions/**")
        archiveBaseName.set("BigDoorsOpener")
    }

    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("26.3")
        downloadPlugins {
            modrinth("bigdoors", "0.1.8.72")
        }
        jvmArgs("-Dcom.mojang.eula.agree=true")
    }
}

val conditionGroups = listOf("item", "location", "worldLocation", "permission", "time", "weather", "placeholder", "mythicMob", "cooldown", "globalCooldown")

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
                "bdo.command.use", "bdo.command.givekey", "bdo.command.reload", "bdo.command.debug", "bdo.customEvaluator",
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
            description = "Reload the plugin"
            default = OP
        }
        register("bdo.command.debug") {
            description = "Upload debug reports"
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
