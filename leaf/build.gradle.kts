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

        filter {
             includeTestsMatching("dugsolutions.leaf.v35.*")
        }
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
    create("simulation") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
    create("simulationTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets["simulation"].output
        runtimeClasspath += sourceSets.main.get().output + sourceSets["simulation"].output
    }
}

kotlin {
    target.compilations.getByName("simulationTest")
        .associateWith(
            target.compilations.getByName("simulation")
        )
}

// Simulation code is an application/research layer on top of the production
// rules engine. It inherits production dependencies but cannot see test or
// integration-only helpers.
configurations["simulationImplementation"].extendsFrom(configurations["implementation"])
configurations["simulationRuntimeOnly"].extendsFrom(configurations["runtimeOnly"])
configurations["simulationTestImplementation"].extendsFrom(
    configurations["simulationImplementation"],
    configurations["testImplementation"]
)
configurations["simulationTestRuntimeOnly"].extendsFrom(
    configurations["simulationRuntimeOnly"],
    configurations["testRuntimeOnly"]
)

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
    description = "Runs v35 integration tests."
    group = "verification"

    testClassesDirs = sourceSets["integration"].output.classesDirs
    classpath = sourceSets["integration"].runtimeClasspath

    useJUnitPlatform()

    listOf(
        "leaf.purchaseVariety.games",
        "leaf.purchaseVariety.commonThreshold",
        "leaf.purchaseVariety.detail"
    ).forEach { propertyName ->
        System.getProperty(propertyName)?.let { systemProperty(propertyName, it) }
    }

    filter {
        includeTestsMatching("dugsolutions.leaf.integration.v35.*")
    }

    dependsOn("test")

    reports {
        html.required.set(true)
        junitXml.required.set(true)
    }

    testLogging {
        events("passed", "skipped", "failed")
    }
}


tasks.register<Test>("v35IntegrationTest") {
    description = "Runs v35 integration tests."
    group = "verification"

    testClassesDirs = sourceSets["integration"].output.classesDirs
    classpath = sourceSets["integration"].runtimeClasspath

    useJUnitPlatform()

    filter {
        includeTestsMatching("dugsolutions.leaf.integration.v35.*")
    }

    dependsOn("test")

    reports {
        html.required.set(true)
        junitXml.required.set(true)
    }

    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Lightweight architecture check for the research source set. Higher-level
// strategies are intentionally scaffolding for now; this task proves that the
// simulation boundary compiles without pulling code into integration tests.
tasks.register<Test>("simulationTest") {
    description = "Runs v35 simulation-layer unit tests."
    group = "verification"

    testClassesDirs = sourceSets["simulationTest"].output.classesDirs
    classpath = sourceSets["simulationTest"].runtimeClasspath

    useJUnitPlatform()

    filter {
        includeTestsMatching("dugsolutions.leaf.simulation.v35.*")
    }

    reports {
        html.required.set(true)
        junitXml.required.set(true)
    }

    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Lightweight architecture + unit check for the research source set.
tasks.register("simulationCheck") {
    description = "Compiles and tests the v35 simulation/research source set."
    group = "verification"
    dependsOn("compileSimulationKotlin", "simulationTest")
}


kotlin {
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

// Runs one complete Mechanical Control game and writes a human-readable
// Chronicle under output/smoke/mechanical-control/. This is a visual engine
// sanity check, not a statistical balance experiment.
tasks.register<JavaExec>("runMechanicalGameSmoke") {
    description = "Runs one full Mechanical Control game and writes its Chronicle to output/."
    group = "verification"

    dependsOn("integrationClasses")
    classpath = sourceSets["integration"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.integration.v35.tool.MechanicalGameSmokeMainKt")

    project.findProperty("smokeSeed")?.toString()?.let { args(it) }
    if (project.findProperty("detail")?.toString()?.toBoolean() == true) {
        args("--detail")
    }
}

// Runs one complete four-player Human Baseline 3/2/2 game and writes both
// summary and compact Chronicle output under output/smoke/human-baseline/.
// Use -Pdetail=true for the full decision-rich diagnostic transcript.
tasks.register<JavaExec>("runHumanBaselineSmoke") {
    description = "Runs one full 3/2/2 Human Baseline game and writes its Chronicle to output/."
    group = "verification"

    dependsOn("integrationClasses")
    classpath = sourceSets["integration"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.integration.v35.tool.HumanBaselineGameSmokeMainKt")

    val smokeSeed = project.findProperty("smokeSeed")?.toString()
    val smokeStrategySeed = project.findProperty("smokeStrategySeed")?.toString()
    if (smokeSeed != null) {
        args(smokeSeed)
        if (smokeStrategySeed != null) args(smokeStrategySeed)
    } else if (smokeStrategySeed != null) {
        args("13579")
        args(smokeStrategySeed)
    }
    project.findProperty("grove")?.toString()?.let { args("--grove=$it") }
    project.findProperty("players")?.toString()?.let { args("--players=$it") }
    if (project.findProperty("detail")?.toString()?.toBoolean() == true) {
        args("--detail")
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

// Human-facing Milestone-3 research runners. These are deliberately separate
// from simulationTest: tests verify the machinery; these tasks produce data
// for a designer to inspect and interpret.
tasks.register<JavaExec>("runBaselineRandomnessDiagnostic") {
    description = "Runs a small Human Baseline cohort and prints per-game development diagnostics."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.experiment.baseline.BaselineResearchMainKt")
    args("diagnostic")
    project.findProperty("games")?.toString()?.let { args("--games=$it") }
    project.findProperty("baseSeed")?.toString()?.let { args("--base-seed=$it") }
    project.findProperty("strategySeed")?.toString()?.let { args("--strategy-seed=$it") }
    project.findProperty("plants")?.toString()?.let { args("--plants=$it") }
    project.findProperty("players")?.toString()?.let { args("--players=$it") }
}

tasks.register<JavaExec>("runBaselineCalibration") {
    description = "Runs cumulative Human Baseline seat calibration and prints aggregate checkpoints."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.experiment.baseline.BaselineResearchMainKt")
    args("calibration")
    project.findProperty("games")?.toString()?.let { args("--games=$it") }
    project.findProperty("checkpoints")?.toString()?.let { args("--checkpoints=$it") }
    project.findProperty("baseSeed")?.toString()?.let { args("--base-seed=$it") }
    project.findProperty("strategySeed")?.toString()?.let { args("--strategy-seed=$it") }
    project.findProperty("plants")?.toString()?.let { args("--plants=$it") }
    project.findProperty("players")?.toString()?.let { args("--players=$it") }
}

tasks.register<JavaExec>("runSixWispExperiment") {
    description = "Runs matched Human Baseline control vs Six-Wisp opening games."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.experiment.sixwisp.SixWispExperimentMainKt")
    project.findProperty("games")?.toString()?.let { args(it) }
    project.findProperty("baseSeed")?.toString()?.let { args("--seed=$it") }
    project.findProperty("strategySeed")?.toString()?.let { args("--strategy-seed=$it") }
    project.findProperty("wisps")?.toString()?.let { args("--wisps=$it") }
    project.findProperty("rounds")?.toString()?.let { args("--rounds=$it") }
    project.findProperty("players")?.toString()?.let { args("--players=$it") }
    project.findProperty("grove")?.toString()?.let { args("--grove=$it") }
    if (project.findProperty("randomGrove")?.toString()?.toBoolean() == true) {
        args("--random-grove")
    }
}

tasks.register<JavaExec>("runFocusedPlantShapeExperiment") {
    description = "Runs matched Human Baseline control vs focused Lean Creature strategy games."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.experiment.FocusedPlantShapeExperimentMainKt")
    project.findProperty("games")?.toString()?.let { args(it) }
    project.findProperty("baseSeed")?.toString()?.let { args("--seed=$it") }
    project.findProperty("strategySeed")?.toString()?.let { args("--strategy-seed=$it") }
    project.findProperty("players")?.toString()?.let { args("--players=$it") }
}

//  transparent linear Buy-policy evolutionary trainer.
tasks.register<JavaExec>("runTrainBuyPolicy") {
    description = "Evolves the learned Buy policy against Human Baseline opponents."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.learning.buy.TrainBuyPolicyMainKt")
    if (project.hasProperty("args")) args(project.property("args").toString().split(" ").filter { it.isNotBlank() })
}

//  matched held-out evaluation for a trained learned Buy policy.
tasks.register<JavaExec>("runEvaluateBuyPolicy") {
    description = "Evaluates a trained learned Buy policy on held-out matched games against Human Baseline."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.learning.buy.EvaluateBuyPolicyMainKt")
    if (project.hasProperty("args")) args(project.property("args").toString().split(" ").filter { it.isNotBlank() })
}

tasks.register<JavaExec>("runTargetedDecisionCalibration") {
    description = "Runs reusable Human Baseline targeted decision calibration observations."
    group = "verification"
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.experiment.decisioncalibration.TargetedDecisionCalibrationMainKt")
    val rawArgs = providers.gradleProperty("args").orNull
    if (!rawArgs.isNullOrBlank()) args(rawArgs.split(Regex("\\s+")).filter(String::isNotBlank))
}

tasks.register<JavaExec>("runHumanBaselineGlobalCertification") {
    description = "Runs substantial all-Human-Baseline global certification cohorts."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.learning.buy.HumanBaselineGlobalCertificationMainKt")
    if (project.hasProperty("args")) args(project.property("args").toString().split(" ").filter { it.isNotBlank() })
}

// Evolves only high-level Cultivation Build Main Action selection.
tasks.register<JavaExec>("runTrainCultivationMainPolicy") {
    description = "Evolves the learned Cultivation Main policy against Human Baseline opponents."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.learning.cultivation.TrainCultivationMainPolicyMainKt")
    if (project.hasProperty("args")) args(project.property("args").toString().split(" ").filter { it.isNotBlank() })
}

// Matched held-out evaluation of one learned Cultivation Main policy.
tasks.register<JavaExec>("runEvaluateCultivationMainPolicy") {
    description = "Evaluates a learned Cultivation Main policy against Human Cultivation Main on matched held-out games."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.learning.cultivation.EvaluateCultivationMainPolicyMainKt")
    if (project.hasProperty("args")) args(project.property("args").toString().split(" ").filter { it.isNotBlank() })
}

// Evolves optional Cultivation Support / Helper timing independently.
tasks.register<JavaExec>("runTrainCultivationSupportPolicy") {
    description = "Evolves the learned Cultivation Support policy against Human Baseline opponents."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.learning.cultivation.support.TrainCultivationSupportPolicyMainKt")
    if (project.hasProperty("args")) args(project.property("args").toString().split(" ").filter { it.isNotBlank() })
}

tasks.register<JavaExec>("runEvaluateCultivationSupportPolicy") {
    description = "Evaluates learned Cultivation Support against Human Cultivation Support on matched held-out games."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.learning.cultivation.support.EvaluateCultivationSupportPolicyMainKt")
    if (project.hasProperty("args")) args(project.property("args").toString().split(" ").filter { it.isNotBlank() })
}

// Evolves only high-level Battle Step-5 Support/final-main selection.
tasks.register<JavaExec>("runTrainBattleSupportPolicy") {
    description = "Evolves the learned Battle Support policy against Human Baseline opponents."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.learning.battle.TrainBattleSupportPolicyMainKt")
    if (project.hasProperty("args")) args(project.property("args").toString().split(" ").filter { it.isNotBlank() })
}

tasks.register<JavaExec>("runEvaluateBattleSupportPolicy") {
    description = "Evaluates a learned Battle Support policy against Human Battle Support on matched held-out games."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.learning.battle.EvaluateBattleSupportPolicyMainKt")
    if (project.hasProperty("args")) args(project.property("args").toString().split(" ").filter { it.isNotBlank() })
}

// Modular held-out evaluator for independently selectable Buy, Cultivation Main,
// and Battle Support policies. Battle Main intentionally remains Human Baseline.
tasks.register<JavaExec>("runTrainWispPolicy") {
    description = "Evolves the learned Wisp Play policy against Human Baseline opponents."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.learning.wisp.TrainWispPlayPolicyMainKt")
    if (project.hasProperty("args")) args(project.property("args").toString().split(" ").filter { it.isNotBlank() })
}

tasks.register<JavaExec>("runEvaluateWispPolicy") {
    description = "Evaluates a learned Wisp Play policy on held-out matched games."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.learning.wisp.EvaluateWispPlayPolicyMainKt")
    if (project.hasProperty("args")) args(project.property("args").toString().split(" ").filter { it.isNotBlank() })
}

tasks.register<JavaExec>("runEvaluatePolicyInteractions") {
    description = "Evaluates modular Buy/Cultivation Main/Battle Support policy combinations on matched schedules."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.learning.interaction.EvaluatePolicyInteractionMainKt")
    if (project.hasProperty("args")) args(project.property("args").toString().split(" ").filter { it.isNotBlank() })
}

tasks.register<JavaExec>("runTrainPlantEffectPolicy") {
    description = "Evolves learned Plant effect/targeting policy against Human Baseline opponents."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.learning.plant.TrainPlantEffectPolicyMainKt")
    if (project.hasProperty("args")) args(project.property("args").toString().split(" ").filter { it.isNotBlank() })
}

tasks.register<JavaExec>("runEvaluatePlantEffectPolicy") {
    description = "Evaluates learned Plant effect/targeting policy on held-out matched games."
    group = "simulation research"
    dependsOn("simulationClasses")
    classpath = sourceSets["simulation"].runtimeClasspath
    mainClass.set("dugsolutions.leaf.simulation.v35.learning.plant.EvaluatePlantEffectPolicyMainKt")
    if (project.hasProperty("args")) args(project.property("args").toString().split(" ").filter { it.isNotBlank() })
}
