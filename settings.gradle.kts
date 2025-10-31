plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
    // Apply AWS CodeArtifact gradle plugin (See: https://github.com/cloudshiftinc/codeartifact-gradle-plugin)
    id("io.cloudshiftdev.codeartifact") version "0.7.4"
}

rootProject.name = "immix-parent"

fun includeBuildIfExist(path: String) {
    val file = file(path)
    if (file.exists()) {
        includeBuild(path)
    }
}

includeBuildIfExist("../immix-trading")
includeBuildIfExist("../immix-babl")
includeBuildIfExist("../immix-common")