plugins {
    id("com.github.johnrengelman.shadow") version "6.0.0"
    java
    `maven-publish`
    `java-library`
}

group = "de.eldoria"
version = "2.4.2"
var mainPackage = "bigdoorsopener"
val shadebade = group as String? + "." + mainPackage + "."
val name = "BigDoorsOpener"
description = "Open and close doors automatically on certain conditions"

val lombokVersion = "1.18.20"

repositories {
    maven("https://eldonexus.de/repository/maven-releases/")
    maven("https://eldonexus.de/repository/maven-proxies/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    implementation("de.eldoria", "eldo-util", "1.9.1")
    implementation("net.kyori", "adventure-api", "4.8.1")
    implementation("net.kyori", "adventure-platform-bukkit", "4.0.0-SNAPSHOT")
    compileOnly("org.spigotmc", "spigot-api", "1.13.2-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains", "annotations", "20.1.0")
    compileOnly("com.sk89q.worldguard", "worldguard-bukkit", "7.0.5-SNAPSHOT") {
        exclude("org.spigotmc", "spigot-api")
    }
    compileOnly("me.clip", "placeholderapi", "2.10.6")
    compileOnly("io.lumine.xikage", "MythicMobs", "4.9.1")
    compileOnly("nl.pim16aap2", "BigDoors", "0.1.8.28")
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.5.2")
    testCompileOnly("org.projectlombok", "lombok", lombokVersion)
    testAnnotationProcessor("org.projectlombok", "lombok", lombokVersion)
}


java {
    withSourcesJar()
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
}

publishing {
    val publishData = PublishData(project)
    publications.create<MavenPublication>("maven") {
        from(components["java"])
        groupId = project.group as String?
        artifactId = project.name.toLowerCase()
        version = publishData.getVersion()
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
        minimize()
        archiveClassifier.set("")
    }

    test {
        useJUnit()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

class PublishData(private val project: Project) {
    private var type: Type = getReleaseType()
    private var hashLength: Int = 7

    private fun getReleaseType(): Type {
        val branch = getCheckedOutBranch()
        return when {
            branch.contentEquals("master") -> Type.RELEASE
            branch.startsWith("dev") -> Type.DEV
            else -> Type.SNAPSHOT
        }
    }

    private fun getCheckedOutGitCommitHash(): String = System.getenv("GITHUB_SHA")?.substring(0, hashLength) ?: "local"

    private fun getCheckedOutBranch(): String = System.getenv("GITHUB_REF")?.replace("refs/heads/", "") ?: "local"

    fun getVersion(): String = getVersion(false)

    fun getVersion(appendCommit: Boolean): String =
        type.append(getVersionString(), appendCommit, getCheckedOutGitCommitHash())

    private fun getVersionString(): String = (project.version as String).replace("-SNAPSHOT", "").replace("-DEV", "")

    fun getRepository(): String = type.repo

    enum class Type(private val append: String, val repo: String, private val addCommit: Boolean) {
        RELEASE("", "https://eldonexus.de/repository/maven-releases/", false),
        DEV("-DEV", "https://eldonexus.de/repository/maven-dev/", true),
        SNAPSHOT("-SNAPSHOT", "https://eldonexus.de/repository/maven-snapshots/", true);

        fun append(name: String, appendCommit: Boolean, commitHash: String): String =
            name.plus(append).plus(if (appendCommit && addCommit) "-".plus(commitHash) else "")
    }
}
