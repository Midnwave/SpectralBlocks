plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.1"
}

group = "com.horizonsmp"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://maven.devs.beer/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    // Paper API — compile against 1.21.4, compatible down to 1.21 at runtime
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // PacketEvents — shaded into the JAR for fake block packet delivery
    implementation("com.github.retrooper:packetevents-spigot:2.11.2")

    // ItemsAdder API — soft-depend; all IA 4.x versions share this API surface
    compileOnly("dev.lone:api-itemsadder:4.0.10")

    // WorldGuard — soft-depend for region flag checks
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.15")

    // bStats — shaded for metrics
    implementation("org.bstats:bstats-bukkit:3.1.0")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    // Relocate shaded libs to avoid conflicts with other plugins
    relocate("com.github.retrooper.packetevents", "com.horizonsmp.spectralblocks.libs.packetevents")
    relocate("io.github.retrooper.packetevents", "com.horizonsmp.spectralblocks.libs.retrooper")
    relocate("org.bstats", "com.horizonsmp.spectralblocks.libs.bstats")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    // Disable the default jar task; shadowJar replaces it
    enabled = false
}
