import org.danilopianini.gradle.mavencentral.JavadocJar
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.support.listFilesOrdered

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.multiJvmTesting)
    alias(libs.plugins.npm.publish)
    alias(libs.plugins.publishOnCentral)
    alias(libs.plugins.taskTree)
}

group = "org.danilopianini"

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        val commonMain by getting { }
        val commonTest by getting {
            dependencies {
                implementation(libs.bundles.kotlin.testing.common)
                implementation(libs.bundles.kotest.common)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.runner.junit5)
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        val nativeTest by creating {
            dependsOn(commonTest)
        }
    }

    js(IR) {
        browser()
        nodejs()
        binaries.library()
    }

    val os = OperatingSystem.current()

    val nativeSetup: KotlinNativeTarget.() -> Unit = {
        compilations["main"].defaultSourceSet.dependsOn(kotlin.sourceSets["nativeMain"])
        compilations["test"].defaultSourceSet.dependsOn(kotlin.sourceSets["nativeTest"])
        binaries {
            staticLib()
            sharedLib()
            all {
                val libPath = "${rootProject.projectDir}/target/release"
                val libName = "plus"
                val libNameInOS = when {
                    os.isLinux || os.isMacOsX -> "lib$libName.a"
                    os.isWindows -> "$libName.lib"
                    else -> ""
                }
                linkerOpts.add("$libPath/$libNameInOS")
            }
            executable {
                entryPoint = "main"
            }
        }
        compilations.getByName("main") {
            cinterops {
                val plus by creating {
                    includeDirs("$projectDir/target")
                }
            }
        }
    }

    linuxX64(nativeSetup)
    mingwX64(nativeSetup)
    macosX64(nativeSetup)

    targets.all {
        compilations.all {
            kotlinOptions {
                allWarningsAsErrors = true
            }
        }
    }

    val excludeTargets = when {
        os.isLinux -> kotlin.targets.filterNot { "linux" in it.name }
        os.isWindows -> kotlin.targets.filterNot { "mingw" in it.name }
        os.isMacOsX -> kotlin.targets.filter { "linux" in it.name || "mingw" in it.name }
        else -> emptyList()
    }.mapNotNull { it as? KotlinNativeTarget }

    configure(excludeTargets) {
        compilations.configureEach {
            cinterops.configureEach { tasks[interopProcessingTaskName].enabled = false }
            compileTaskProvider.get().enabled = false
            tasks[processResourcesTaskName].enabled = false
        }
        binaries.configureEach { linkTask.enabled = false }

        mavenPublication {
            tasks.withType<AbstractPublishToMaven>().configureEach {
                onlyIf { publication != this@mavenPublication }
            }
            tasks.withType<GenerateModuleMetadata>().configureEach {
                onlyIf { publication.get() != this@mavenPublication }
            }
        }
    }
}

tasks.dokkaJavadoc {
    enabled = false
}

tasks.withType<JavadocJar>().configureEach {
    val dokka = tasks.dokkaHtml.get()
    dependsOn(dokka)
    from(dokka.outputDirectory)
}

signing {
    if (System.getenv("CI") == "true") {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}

publishOnCentral {
    projectLongName.set("Template for Kotlin Multiplatform Project")
    projectDescription.set("A template repository for Kotlin Multiplatform projects")
    repository("https://maven.pkg.github.com/danysk/${rootProject.name}".lowercase()) {
        user.set("DanySK")
        password.set(System.getenv("GITHUB_TOKEN"))
    }
    publishing {
        publications {
            withType<MavenPublication> {
                pom {
                    developers {
                        developer {
                            name.set("Danilo Pianini")
                            email.set("danilo.pianini@gmail.com")
                            url.set("http://www.danilopianini.org/")
                        }
                    }
                }
            }
        }
    }
}

npmPublish {
    registries {
        register("npmjs") {
            uri.set("https://registry.npmjs.org")
            val npmToken: String? by project
            authToken.set(npmToken)
            dry.set(npmToken.isNullOrBlank())
        }
    }
}

publishing {
    publications {
        publications.withType<MavenPublication>().configureEach {
            if ("OSSRH" !in name) {
                artifact(tasks.javadocJar)
            }
        }
    }
}

// *****************************
//      Rust Configurations
// *****************************

/**
 * Utility to launch cargo commands. Might be moved to buildSrc.
 */
fun Task.cargoCLI(vararg commands: String) {
    doLast {
        project.exec {
            commandLine("cargo", *commands)
        }
    }
}

/**
 * Generate the .h files to bind the Rust .a library to Kotlin.
 * See the .def file in the nativeInterop folder to see the cinterop configuration.
 */
val generateHeaders by tasks.registering {
    doLast {
        project.exec {
            commandLine("cbindgen", "--output", "target/plus.h")
        }
    }
}

/**
 * Build the Rust library with release optimization enabled in the compiler.
 */
val cargoBuildRelease by tasks.registering {
    cargoCLI("build", "--release")
    finalizedBy(generateHeaders)
    doLast {
        println(File("${rootProject.projectDir}/target/release").listFilesOrdered().map { it.name })
    }
}

/**
 * Execute the Rust tests.
 */
val cargoTest by tasks.registering {
    cargoCLI("test")
    dependsOn(cargoBuildRelease)
}

/**
 * Tasks relative to cinterop needs to Rust library to correctly execute.
 */
tasks.filter { it.name.contains("cinterop") }.forEach {
    it.dependsOn(cargoBuildRelease)
}

/**
 * Also run Rust tests when launching the "check" task.
 */
tasks.check {
    dependsOn(cargoTest)
}

/**
 * The clean task also delete the "target" folder, equivalent of "build" folder in kotlin for Rust.
 */
tasks.clean {
    doFirst {
        delete("target")
    }
}
