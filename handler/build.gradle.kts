import java.util.Date
import java.util.Properties

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("maven-publish")
    id("com.jfrog.bintray")
}

val organization = "darkosinc"
val repository = "validation"

val artifactName = "effect-handler"
val artifactGroup = "com.$organization.$repository"
val artifactVersion = "0.0.2"

group = artifactGroup
version = artifactVersion

repositories {
    gradlePluginPortal()
    google()
    maven(url = "https://dl.google.com/dl/android/maven2")
    jcenter()
    mavenCentral()
    maven(url = "https://dl.bintray.com/darkosinc/MVU")
    maven(url = "https://dl.bintray.com/darkosinc/validation")
}

android {
    val sdkMin = 23
    val sdkCompile = 30

    compileSdkVersion(sdkCompile)
    defaultConfig {
        minSdkVersion(sdkMin)
        targetSdkVersion(sdkCompile)
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("com.darkosinc.MVU:core-android:0.0.6")
    implementation("com.darkosinc.validation:api-android:0.0.2")
}

kotlin {
    android("android") {
        publishLibraryVariants("release")
    }
    ios {
        binaries {
            framework {
                baseName = artifactName
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
                implementation("com.darkosinc.MVU:core:0.0.6")
                implementation("com.darkosinc.validation:api:0.0.2")
            }
        }
        val androidMain by getting
        val iosMain by getting
        val iosArm64Main by getting {
            dependencies {
                implementation("com.darkosinc.MVU:core-iosArm64:0.0.6")
                implementation("com.darkosinc.validation:api-iosArm64:0.0.2")
            }
        }
        val iosX64Main by getting {
            dependencies {
                implementation("com.darkosinc.MVU:core-iosX64:0.0.6")
                implementation("com.darkosinc.validation:api-iosX64:0.0.2")
            }
        }
    }
}

afterEvaluate {
    publishing.publications.all {
            if(this is MavenPublication){
                groupId = artifactGroup

                artifactId = when(name){
                    "metadata" -> artifactName
                    "androidRelease" -> "$artifactName-android"
                    else -> "$artifactName-$name"
                }
            }
        }
}

fun getLocalProperties(): Properties {
    return Properties().apply {
        load(project.rootProject.file("local.properties").inputStream())
    }
}

bintray {
    val p = getLocalProperties()

    user = p.getProperty("bintrayUser")
    key = p.getProperty("bintrayKey")
    publish = false

    pkg.apply {
        repo = repository
        name = artifactName
        userOrg = organization

        version.apply {
            name = artifactVersion
            released = Date().toString()
            vcsTag = artifactVersion
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("metadata"){
            artifactId = artifactName
            groupId = artifactGroup

            from(components.getByName("kotlin"))

            pom {
                name.set("effect-handler")
                description.set("description")
                url.set("https://github.com/Darkos-den/validation")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

            }
        }
    }
}

tasks.getByName<com.jfrog.bintray.gradle.tasks.BintrayUploadTask>("bintrayUpload"){
    doFirst {
        publishing.publications.asMap.keys
            .filter { it != "kotlinMultiplatform" }
            .toTypedArray()
            .let {
                setPublications(*it)
            }
    }
}

tasks.getByName("bintrayUpload").dependsOn(tasks.getByName("publishToMavenLocal").path)