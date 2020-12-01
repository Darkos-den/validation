pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        jcenter()
        mavenCentral()
    }
}
rootProject.name = "validation-handler"

enableFeaturePreview("GRADLE_METADATA")

include(":handler")

