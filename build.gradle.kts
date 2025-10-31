import io.cloudshiftdev.gradle.codeartifact.awsCodeArtifact

plugins {
    java
    `maven-publish`
    checkstyle
    jacoco
}

// dynamically resolve java version
fun resolveBuildJavaVersion(): Int {
    var buildJavaVersion = System.getenv("BUILD_JAVA_VERSION") ?: JavaVersion.current().majorVersion

    // Remove the part after '.' if present
    if (buildJavaVersion.contains('.')) {
        buildJavaVersion = buildJavaVersion.substring(0, buildJavaVersion.indexOf('.'))
    }

    // Remove the part after '-' if present
    if (buildJavaVersion.contains('-')) {
        buildJavaVersion = buildJavaVersion.substring(0, buildJavaVersion.indexOf('-'))
    }

    return buildJavaVersion.toInt()
}

// Usage
val buildJavaVersion: Int = resolveBuildJavaVersion()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(buildJavaVersion))
    }
    sourceCompatibility = JavaVersion.VERSION_17
}

group = "xyz.immix.trading"

defaultTasks("check", "build", "test", "uberJar")

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:3.1.5")
    }
}

allprojects {
    apply(plugin = "java-library")
    apply(plugin = "jacoco")

    repositories {
        mavenLocal()
        mavenCentral()
        google()

    }

    tasks.withType<Test>().configureEach {
        jvmArgs(
            "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens", "java.base/java.util=ALL-UNNAMED",
            "--add-opens", "java.base/java.nio=ALL-UNNAMED",
            "--add-opens", "java.base/jdk.internal.math=ALL-UNNAMED",
        )
        if (buildJavaVersion >= 21) {
            jvmArgs("-XX:+EnableDynamicAgentLoading")
        }
    }

    tasks.compileJava {
        options.compilerArgs.addAll(
            listOf(
                "--add-exports", "java.base/sun.nio.ch=ALL-UNNAMED",
                "--add-exports", "java.base/java.util=ALL-UNNAMED",
                "--add-exports", "java.base/java.nio=ALL-UNNAMED",
                "--add-exports", "java.base/jdk.internal.math=ALL-UNNAMED"
            )
        )
    }

    tasks.test {
        useJUnitPlatform()
        jvmArgs(
            "--add-exports", "java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens", "java.base/java.nio=ALL-UNNAMED",
            "--add-opens", "java.base/jdk.internal.math=ALL-UNNAMED"
        )
        finalizedBy(tasks.jacocoTestReport)
    }

    tasks.jacocoTestReport {
        reports.xml.required.set(true)
        dependsOn(tasks.test)
    }

    tasks.jacocoTestCoverageVerification {
        group = "Verifications"
        violationRules {
            rule {
                isEnabled = false
                limit {
                    minimum = "0.2".toBigDecimal() // 20% minimum coverage
                }
            }
        }
    }

    tasks.register("testCoverage") {
        group = "Verification"
        description = "Runs tests, generates coverage reports, and verifies coverage metrics."
        dependsOn(tasks.jacocoTestReport, tasks.jacocoTestCoverageVerification)
    }

    tasks.jar {
        // Remove any multi-release JAR configuration
        manifest {
            attributes.remove("Multi-Release")
        }

        // Do not include any version-specific directories
        rootSpec.excludes.add("META-INF/versions/**")
    }

}

subprojects {
    apply(plugin = "eclipse")
    apply(plugin = "checkstyle")
    apply(plugin = "jacoco")
    apply(plugin = "maven-publish")

    dependencies {
        implementation(rootProject.project.libs.lombok)
        implementation(rootProject.project.libs.googleGuava)
        implementation(rootProject.project.libs.bundles.log4j2)
        implementation(rootProject.project.libs.springBootLoader)
        testImplementation(rootProject.project.libs.bundles.testing)
        annotationProcessor(rootProject.project.libs.lombok)
        testAnnotationProcessor(rootProject.project.libs.lombok)
    }

    checkstyle {
        maxWarnings = 0
        toolVersion = rootProject.project.libs.versions.checkstyleVersion.get()
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:unchecked")
        options.compilerArgs.add("-Xlint:deprecation")
        targetCompatibility = "17"
        sourceCompatibility = "17"
    }

    tasks.publish {
        dependsOn(tasks.test)
    }

    publishing {
        publications {
            // Remove default publication if present
            withType<MavenPublication>().configureEach {
                if (name == "mavenJava") {
                    // Skip or remove mavenJava publication to avoid conflicts
                    tasks.withType<AbstractPublishToMaven>().configureEach {
                        onlyIf { publication != this@configureEach }
                    }
                }
            }
        }
        repositories {
            awsCodeArtifact(
                url = System.getenv("CODEARTIFACT_TRADING_V2_MAVEN_URL")
                    ?: "https://immix-develop-908032705112.d.codeartifact.eu-west-1.amazonaws.com/maven/immix-trading-v2/"
            )
            mavenLocal()
        }
    }
}
