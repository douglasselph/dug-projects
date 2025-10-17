plugins {
    kotlin("jvm") version "1.9.21"
    id("org.jetbrains.compose") version "1.5.11"  // For your desktop UI components
}

group = "dugsolutions.leaf"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.21"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    implementation(kotlin("reflect"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.uiTooling)
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3") // For Swing UI thread integration
    
    // Koin
    val koinVersion = "3.5.3"
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-core-coroutines:$koinVersion")
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("io.mockk:mockk:1.13.8")  // Add MockK for mocking
    testImplementation("io.github.serpro69:kotlin-faker:1.15.0")  // Add kfaker for test data generation
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    // Configure test task to use JUnit Platform
    tasks.test {
        useJUnitPlatform()
    }
}

sourceSets {
    test {
        resources {
            srcDirs("data")
        }
    }
    create("integration") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

compose.desktop {
    application {
        mainClass = "dugsolutions.leaf.MainKt"
    }
}

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    "integrationImplementation"("org.junit.jupiter:junit-jupiter:5.8.2")
    "integrationImplementation"("org.junit.vintage:junit-vintage-engine:5.8.2")
    "integrationImplementation"("io.insert-koin:koin-test:3.5.3")
    "integrationImplementation"("io.insert-koin:koin-test-junit5:3.5.3")
    "integrationRuntimeOnly"("org.junit.platform:junit-platform-launcher:1.8.2")
    "integrationImplementation"("io.mockk:mockk:1.13.8")
    "integrationImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    "integrationImplementation"(compose.runtime)
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    
    testClassesDirs = sourceSets["integration"].output.classesDirs
    classpath = sourceSets["integration"].runtimeClasspath
    
    useJUnitPlatform()
    
    // Ensure integration tests run after unit tests
    dependsOn("test")
    
    // Set up test reporting
    reports {
        html.required.set(true)
        junitXml.required.set(true)
    }
    
    // Configure test logging
    testLogging {
        events("passed", "skipped", "failed")
    }
}

kotlin {
    // Common compiler options
    jvmToolchain(11)
    
    // Specific compiler options for integration tests
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        if (name.contains("compileIntegrationKotlin")) {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xplugin-disable=androidx.compose.compiler.plugins.kotlin"
                )
            }
        }
    }
}

// Add custom task to run SimpleTestRunner
tasks.register<JavaExec>("runSimpleTestRunner") {
    description = "Runs the SimpleTestRunner to view test output"
    group = "application"
    
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.tool.SimpleTestRunnerKt")
}

// Add task for running TestOutputViewer
tasks.register<JavaExec>("viewTestOutput") {
    description = "Runs the TestOutputViewer to view test output files"
    group = "application"
    
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.tool.TestOutputViewerKt")
    
    // Allow command line arguments to be passed
    args = project.findProperty("args")?.toString()?.split("\\s+".toRegex()) ?: listOf()
} 

