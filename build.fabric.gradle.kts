plugins {
    id("dev.kikugie.loom-back-compat")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id")}-fabric"

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }

repositories {
    maven("https://maven.shedaniel.me/") { name = "Shedaniel" }
    maven("https://maven.terraformersmc.com/releases/") { name = "TerraformersMC" }
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("universal-emotes") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("runs/fabric/${sc.current.version}")
        if (project.hasProperty("mixinAudit")) vmArg("-Duniversalemotes.mixinAudit=true")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    modImplementation("me.shedaniel.cloth:cloth-config-fabric:${property("deps.cloth_config")}") {
        exclude(group = "net.fabricmc.fabric-api")
    }
    modImplementation("com.terraformersmc:modmenu:${property("deps.modmenu")}") {
        isTransitive = false
    }

    implementation("io.github.llamalad7:mixinextras-fabric:0.4.1")
    include("io.github.llamalad7:mixinextras-fabric:0.4.1")

    if (sc.current.parsed < "1.19.3") {
        implementation("org.joml:joml:1.10.5")
        include("org.joml:joml:1.10.5")
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, value: String) {
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", sc.properties["mod.id"])
            register("name", sc.properties["mod.name"])
            register("version", version.toString())
            register("minecraft", sc.properties["mod.mc_compat"])
            register("java", requiredJava.majorVersion)
        }

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json5") {
            expand("java" to mixinJava)
            name = name.removeSuffix("5")
            var inBlockComment = false
            filter { line ->
                val out = StringBuilder()
                var i = 0
                while (i < line.length) {
                    if (inBlockComment) {
                        val end = line.indexOf("*/", i)
                        if (end < 0) i = line.length else { inBlockComment = false; i = end + 2 }
                    } else if (line.startsWith("/*", i)) {
                        inBlockComment = true; i += 2
                    } else if (line.startsWith("//", i)) {
                        break
                    } else {
                        out.append(line[i]); i++
                    }
                }
                out.toString()
            }
        }
    }

    withType<Jar> {
        val name = project.property("mod.id")
        inputs.property("mod_id", name)
        from(rootProject.file("LICENSE")) { rename { "${it}_$name" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to `build/libs/` as {mod}-{mod version}-{minecraft}-{loader}.jar"

        val jarName = "${project.property("mod.id")}-${project.property("mod.version")}-${sc.current.version}-fabric.jar"
        inputs.property("jarName", jarName)
        from(loomx.modJar.flatMap { it.archiveFile }) { rename { jarName } }
        into(rootProject.layout.buildDirectory.dir("libs"))
    }
}
