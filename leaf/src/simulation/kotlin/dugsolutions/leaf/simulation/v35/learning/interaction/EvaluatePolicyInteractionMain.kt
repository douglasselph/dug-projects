package dugsolutions.leaf.simulation.v35.learning.interaction

import dugsolutions.leaf.simulation.v35.analysis.GameSummary
import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.SimulationRunContext
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.withSimulationFailureDiagnostics
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.resolveResearchGroveForSample
import dugsolutions.leaf.simulation.v35.experiment.round.RoundExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.learning.buy.parseRoundSetup
import dugsolutions.leaf.simulation.v35.learning.cultivation.affectedSeat
import dugsolutions.leaf.simulation.v35.learning.cultivation.loadCultivationResearchCards
import dugsolutions.leaf.simulation.v35.learning.plant.effectivePlantEffectCatalogCards
import dugsolutions.leaf.v35.chronicle.domain.*
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselineDecisionDirector
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportCatalog
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportPolicy
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportWeights
import dugsolutions.leaf.v35.player.decision.learned.battle.main.LearnedBattleMainCatalog
import dugsolutions.leaf.v35.player.decision.learned.battle.main.LearnedBattleMainPolicy
import dugsolutions.leaf.v35.player.decision.learned.battle.main.LearnedBattleMainWeights
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyCardCatalog
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyStrategy
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainCatalog
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainPolicy
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportCatalog
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportPolicy
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportWeights
import dugsolutions.leaf.v35.player.decision.learned.wisp.LearnedWispPlayCatalog
import dugsolutions.leaf.v35.player.decision.learned.wisp.LearnedWispPlayPolicy
import dugsolutions.leaf.v35.player.decision.learned.wisp.LearnedWispPlayWeights
import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectCatalog
import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectPolicy
import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectWeights
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.tokens.SharedTokenResource
import org.koin.dsl.koinApplication
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal data class CompletedInteractionGame(val summary: GameSummary, val entries: List<GameEntry>)

data class PolicyInteractionOptions(
    val games: Int,
    val seed: Long,
    val strategySeed: Long,
    val grovePattern: String?,
    val groveSeed: Long,
    val plantOverrides: Path?,
    val roundOverrides: Path?,
    val players: Int,
    val roundLabel: String,
    val buyPolicy: String,
    val buyWeights: Path,
    val cultivationMainPolicy: String,
    val cultivationMainWeights: Path,
    val cultivationSupportPolicy: String,
    val cultivationSupportWeights: Path,
    val wispPolicy: String,
    val wispWeights: Path,
    val plantEffectPolicy: String,
    val plantEffectWeights: Path,
    val battleSupportPolicy: String,
    val battleSupportWeights: Path,
    val battleMainPolicy: String,
    val battleMainWeights: Path,
    val configOutput: Path? = null,
    val verbosePlantTargeting: Boolean = false
) {
    val roundSetup = parseRoundSetup(roundLabel)

    fun validateRequiredFiles() {
        plantOverrides?.let { require(Files.isRegularFile(it)) { "Plant override file does not exist: $it" } }
        roundOverrides?.let { require(Files.isRegularFile(it)) { "Round override file does not exist: $it" } }
        val learned = listOf(
            "Buy" to (buyPolicy to buyWeights),
            "Cultivation Main" to (cultivationMainPolicy to cultivationMainWeights),
            "Cultivation Support" to (cultivationSupportPolicy to cultivationSupportWeights),
            "Wisp" to (wispPolicy to wispWeights),
            "Plant Effect" to (plantEffectPolicy to plantEffectWeights),
            "Battle Support" to (battleSupportPolicy to battleSupportWeights),
            "Battle Main" to (battleMainPolicy to battleMainWeights)
        )
        learned.filter { it.second.first == "learned" }.forEach { (name, pair) ->
            require(Files.isRegularFile(pair.second)) { "$name policy is learned but weight file does not exist: ${pair.second}" }
        }
    }

    companion object {
        fun parse(args: List<String>): PolicyInteractionOptions {
            var games = 300
            var seed = 301000L
            var strategySeed = 302000L
            var grovePattern: String? = GrovePlantCode.RANDOM_PATTERN
            var groveSeed = 303000L
            var plantOverrides: Path? = null
            var roundOverrides: Path? = null
            var players = 4
            var roundLabel = "3/2/2"
            var buyPolicy = "human"
            var buyWeights = Paths.get("output/ai/buy-policy-v1-trained.weights")
            var cultivationMainPolicy = "human"
            var cultivationMainWeights = Paths.get("output/ai/cultivation-main-policy-v1-trained.weights")
            var cultivationSupportPolicy = "human"
            var cultivationSupportWeights = Paths.get("output/ai/cultivation-support-policy-v1-trained.weights")
            var wispPolicy = "human"
            var wispWeights = Paths.get("output/ai/wisp-play-policy-v1-trained.weights")
            var plantEffectPolicy = "human"
            var plantEffectWeights = Paths.get("output/ai/plant-effect-policy-v1-trained.weights")
            var battleSupportPolicy = "human"
            var battleSupportWeights = Paths.get("output/ai/battle-support-policy-v1-trained.weights")
            var battleMainPolicy = "human"
            var battleMainWeights = Paths.get("output/ai/battle-main-policy-v1-trained.weights")
            var configOutput: Path? = null
            var verbosePlantTargeting = false
            var i = 0
            fun value(arg: String): String = if ('=' in arg) arg.substringAfter('=') else args[++i]
            while (i < args.size) {
                val arg = args[i]
                when {
                    arg.startsWith("--games") || arg.startsWith("--samples") -> games = value(arg).toInt()
                    arg.startsWith("--seed") -> seed = value(arg).toLong()
                    arg.startsWith("--strategy-seed") -> strategySeed = value(arg).toLong()
                    arg.startsWith("--grove-seed") -> groveSeed = value(arg).toLong()
                    arg.startsWith("--plant-overrides") -> plantOverrides = Paths.get(value(arg))
                    arg.startsWith("--round-overrides") -> roundOverrides = Paths.get(value(arg))
                    arg.startsWith("--players") -> players = value(arg).toInt()
                    arg.startsWith("--rounds") -> roundLabel = value(arg)
                    arg.startsWith("--buy-policy") -> buyPolicy = value(arg).lowercase()
                    arg.startsWith("--buy-weights") -> buyWeights = Paths.get(value(arg))
                    arg.startsWith("--cultivation-main-policy") -> cultivationMainPolicy = value(arg).lowercase()
                    arg.startsWith("--cultivation-main-weights") -> cultivationMainWeights = Paths.get(value(arg))
                    arg.startsWith("--cultivation-support-policy") -> cultivationSupportPolicy = value(arg).lowercase()
                    arg.startsWith("--cultivation-support-weights") -> cultivationSupportWeights = Paths.get(value(arg))
                    arg.startsWith("--wisp-policy") -> wispPolicy = value(arg).lowercase()
                    arg.startsWith("--wisp-weights") -> wispWeights = Paths.get(value(arg))
                    arg.startsWith("--plant-effect-policy") -> plantEffectPolicy = value(arg).lowercase()
                    arg.startsWith("--plant-effect-weights") -> plantEffectWeights = Paths.get(value(arg))
                    arg.startsWith("--battle-support-policy") -> battleSupportPolicy = value(arg).lowercase()
                    arg.startsWith("--battle-support-weights") -> battleSupportWeights = Paths.get(value(arg))
                    arg.startsWith("--battle-main-policy") -> battleMainPolicy = value(arg).lowercase()
                    arg.startsWith("--battle-main-weights") -> battleMainWeights = Paths.get(value(arg))
                    arg.startsWith("--config-output") -> configOutput = Paths.get(value(arg))
                    arg == "--verbose-plant-targeting" -> verbosePlantTargeting = true
                    arg.startsWith("--grove") -> grovePattern = GrovePlantCode.validate(value(arg))
                    arg == "--random-grove" -> grovePattern = GrovePlantCode.RANDOM_PATTERN
                    arg == "--first-game-grove" -> grovePattern = null
                    arg == "--help" -> { usage(); kotlin.system.exitProcess(0) }
                    else -> error("Unknown argument: $arg")
                }
                i++
            }
            require(games > 0) { "--samples/--games must be positive" }
            require(players in 2..4) { "--players must be 2, 3, or 4" }
            require(buyPolicy in setOf("human", "learned")) { "--buy-policy must be human or learned" }
            require(cultivationMainPolicy in setOf("human", "learned")) { "--cultivation-main-policy must be human or learned" }
            require(cultivationSupportPolicy in setOf("human", "learned")) { "--cultivation-support-policy must be human or learned" }
            require(wispPolicy in setOf("human", "learned")) { "--wisp-policy must be human or learned" }
            require(plantEffectPolicy in setOf("human", "learned")) { "--plant-effect-policy must be human or learned" }
            require(battleSupportPolicy in setOf("human", "learned")) { "--battle-support-policy must be human or learned" }
            require(battleMainPolicy in setOf("human", "learned")) { "--battle-main-policy must be human or learned" }
            return PolicyInteractionOptions(
                games = games, seed = seed, strategySeed = strategySeed, grovePattern = grovePattern, groveSeed = groveSeed,
                plantOverrides = plantOverrides, roundOverrides = roundOverrides, players = players, roundLabel = roundLabel,
                buyPolicy = buyPolicy, buyWeights = buyWeights, cultivationMainPolicy = cultivationMainPolicy, cultivationMainWeights = cultivationMainWeights,
                cultivationSupportPolicy = cultivationSupportPolicy, cultivationSupportWeights = cultivationSupportWeights,
                wispPolicy = wispPolicy, wispWeights = wispWeights, plantEffectPolicy = plantEffectPolicy, plantEffectWeights = plantEffectWeights,
                battleSupportPolicy = battleSupportPolicy, battleSupportWeights = battleSupportWeights,
                battleMainPolicy = battleMainPolicy, battleMainWeights = battleMainWeights, configOutput = configOutput,
                verbosePlantTargeting = verbosePlantTargeting
            )
        }

        private fun usage() {
            println("evaluate_policy_interactions [--samples N] [--seed N] [--strategy-seed N] [--grove-seed N] [--random-grove|--first-game-grove|--grove CODE] [--plant-overrides PATH] [--round-overrides PATH] [--players 2|3|4] [--rounds PATTERN] [--config-output PATH] [--verbose-plant-targeting] --buy-policy human|learned [--buy-weights PATH] --cultivation-main-policy human|learned [--cultivation-main-weights PATH] --cultivation-support-policy human|learned [--cultivation-support-weights PATH] --wisp-policy human|learned [--wisp-weights PATH] --plant-effect-policy human|learned [--plant-effect-weights PATH] --battle-support-policy human|learned [--battle-support-weights PATH] --battle-main-policy human|learned [--battle-main-weights PATH]")
        }
    }
}

fun main(args: Array<String>) {
    val o = PolicyInteractionOptions.parse(args.toList())
    o.validateRequiredFiles()
    val app = koinApplication { modules(appModules) }
    try {
        val koin = app.koin
        loadCultivationResearchCards(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
        val plantManager = koin.get<PlantCardManager>()
        val roundManager = koin.get<RoundCardManager>()
        val plants = plantManager.getAllCards().cards
        val rounds = roundManager.getAllCards().cards
        val defaults = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
        val plantExp = PlantExperimentResearchConfig.resolve(o.plantOverrides, plants)
        val roundExp = RoundExperimentResearchConfig.resolve(o.roundOverrides, rounds)
        val effectiveCosts = plantExp.effectiveCosts(plants)

        val buyWeights = if (o.buyPolicy == "learned") LearnedBuyCardCatalog.prepare(LearnedBuyWeights.load(o.buyWeights), plants, effectiveCosts) else null
        val cultivationWeights = if (o.cultivationMainPolicy == "learned") LearnedCultivationMainCatalog.prepare(LearnedCultivationMainWeights.load(o.cultivationMainWeights), plants, effectiveCosts) else null
        val cultivationSupportWeights = if (o.cultivationSupportPolicy == "learned") LearnedCultivationSupportCatalog.prepare(LearnedCultivationSupportWeights.load(o.cultivationSupportWeights), plants) else null
        val wispWeights = if (o.wispPolicy == "learned") LearnedWispPlayCatalog.prepare(LearnedWispPlayWeights.load(o.wispWeights), koin.get<dugsolutions.leaf.v35.wisp.WispCardManager>().getAllCards().cards) else null
        val effectivePlantEffectCatalog = effectivePlantEffectCatalogCards(plants, plantExp.values)
        val plantEffectWeights = if (o.plantEffectPolicy == "learned") LearnedPlantEffectCatalog.prepare(LearnedPlantEffectWeights.load(o.plantEffectWeights), effectivePlantEffectCatalog) else null
        val supportWeights = if (o.battleSupportPolicy == "learned") LearnedBattleSupportCatalog.prepare(LearnedBattleSupportWeights.load(o.battleSupportWeights), plants) else null
        val battleMainWeights = if (o.battleMainPolicy == "learned") LearnedBattleMainCatalog.prepare(LearnedBattleMainWeights.load(o.battleMainWeights), effectivePlantEffectCatalogCards(plants, plantExp.values)) else null
        if (buyWeights != null) LearnedBuyCardCatalog.validateCurrentSchema(buyWeights, plants)
        if (cultivationWeights != null) LearnedCultivationMainCatalog.validateCurrentSchema(cultivationWeights, plants, effectiveCosts)
        if (cultivationSupportWeights != null) LearnedCultivationSupportCatalog.validateCurrentSchema(cultivationSupportWeights, plants)
        if (plantEffectWeights != null) LearnedPlantEffectCatalog.validateCurrentSchema(plantEffectWeights, effectivePlantEffectCatalog)
        if (supportWeights != null) LearnedBattleSupportCatalog.validateCurrentSchema(supportWeights, plants)
        if (battleMainWeights != null) LearnedBattleMainCatalog.validateCurrentSchema(battleMainWeights, effectivePlantEffectCatalog)

        val policyFactory = modularFactory(buyWeights, cultivationWeights, cultivationSupportWeights, wispWeights, plantEffectWeights, supportWeights, battleMainWeights)
        val gameFactory = koin.get<GameFactory>()
        val runner = koin.get<GameRunner>()
        val acc = InteractionAccumulator()

        val metadata = interactionMetadata(o)
        println("Leaf & Let Die — Modular Policy Interaction Evaluation")
        println(renderPolicyConfiguration(metadata))
        o.configOutput?.let { path ->
            path.parent?.let(Files::createDirectories)
            Files.writeString(path, renderMachineConfig(metadata))
            println("config-output=$path")
        }
        if (plantExp.isActive) { println(); println(plantExp.render(plants)) }
        if (roundExp.isActive) { println(); println(roundExp.render(rounds)) }
        println()

        val evaluationStartedNanos = System.nanoTime()
        val progressEvery = maxOf(1, o.games / 10)

        repeat(o.games) { sample ->
            val seat = affectedSeat(sample, o.players)
            val grove = resolveResearchGroveForSample(o.grovePattern, o.groveSeed, sample, plantManager, defaults, plants, plantExp.values)
            val factories = List(o.players) { index -> if (index == seat) policyFactory else PlayerDecisionFactory.humanBaseline() }
            val game = gameFactory(GameConfig(
                selectedPlantCards = grove,
                playerDecisionFactories = factories,
                roundSetup = o.roundSetup,
                seed = o.seed + sample,
                strategySeed = o.strategySeed + sample,
                plantValues = plantExp.values,
                roundValues = roundExp.values
            ))
            val result = withSimulationFailureDiagnostics(
                game,
                SimulationRunContext("evaluate_policy_interactions", sample, "B${o.buyPolicy.first()}-C${o.cultivationMainPolicy.first()}-CS${o.cultivationSupportPolicy.first()}-W${o.wispPolicy.first()}-P${o.plantEffectPolicy.first()}-S${o.battleSupportPolicy.first()}-M${o.battleMainPolicy.first()}", seat, o.seed+sample, o.strategySeed+sample, GrovePlantCode.describe(grove), o.roundLabel)
            ) { runner.run(game) }
            acc.add(CompletedInteractionGame(GameSummaryExtractor.extract(game, result), game.chronicle.entries.toList()), seat)

            val completed = sample + 1
            if (completed % progressEvery == 0 || completed == o.games) {
                printEvaluationProgress(completed, o.games, evaluationStartedNanos)
            }
        }
        printReport(acc, o.games, o.verbosePlantTargeting)
    } finally { app.close() }
}

internal fun printEvaluationProgress(completed: Int, total: Int, startedNanos: Long) {
    val elapsedSeconds = (System.nanoTime() - startedNanos).coerceAtLeast(0L) / 1_000_000_000.0
    val secondsPerGame = if (completed > 0) elapsedSeconds / completed else 0.0
    val remainingSeconds = secondsPerGame * (total - completed).coerceAtLeast(0)
    val eta = ZonedDateTime.now().plusSeconds(remainingSeconds.toLong())
    val percent = if (total > 0) completed * 100.0 / total else 100.0

    println(
        "Evaluation progress: $completed/$total (${String.format("%.1f", percent)}%); " +
            "elapsed ${formatEvaluationDuration(elapsedSeconds)}; " +
            "estimated remaining ${formatEvaluationDuration(remainingSeconds)}; " +
            "ETA ${eta.format(DateTimeFormatter.ofPattern("EEE MMM dd hh:mm a"))}"
    )
}

internal fun formatEvaluationDuration(seconds: Double): String {
    val wholeSeconds = seconds.toLong().coerceAtLeast(0L)
    val duration = Duration.ofSeconds(wholeSeconds)
    val hours = duration.toHours()
    val minutes = duration.minusHours(hours).toMinutes()
    val secs = duration.minusHours(hours).minusMinutes(minutes).seconds
    return when {
        hours > 0 -> String.format("%dh %02dm %02ds", hours, minutes, secs)
        minutes > 0 -> String.format("%dm %02ds", minutes, secs)
        else -> "${secs}s"
    }
}

internal fun modularFactory(
    buyWeights: LearnedBuyWeights?,
    cultivationWeights: LearnedCultivationMainWeights?,
    cultivationSupportWeights: LearnedCultivationSupportWeights?,
    wispWeights: LearnedWispPlayWeights?,
    plantEffectWeights: LearnedPlantEffectWeights?,
    supportWeights: LearnedBattleSupportWeights?,
    battleMainWeights: LearnedBattleMainWeights?
): PlayerDecisionFactory = object : PlayerDecisionFactory {
    override fun create() = create(StrategyRandomizer.create(), DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer: StrategyRandomizer) = create(strategyRandomizer, DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer: StrategyRandomizer, reasoningSink: DecisionReasoningSink) =
        HumanBaselineDecisionDirector(strategyRandomizer = strategyRandomizer, reasoningSink = reasoningSink).createDirector().let { baseline ->
            baseline.copy(
                buy = buyWeights?.let { LearnedBuyStrategy(it, baseline.buy) } ?: baseline.buy,
                cultivationMain = cultivationWeights?.let { LearnedCultivationMainPolicy(it) } ?: baseline.cultivationMain,
                cultivationSupport = cultivationSupportWeights?.let { LearnedCultivationSupportPolicy(it) } ?: baseline.cultivationSupport,
                wispPlay = wispWeights?.let { LearnedWispPlayPolicy(it) } ?: baseline.wispPlay,
                plantEffect = plantEffectWeights?.let { LearnedPlantEffectPolicy(it) } ?: baseline.plantEffect,
                battleSupport = supportWeights?.let { LearnedBattleSupportPolicy(it) } ?: baseline.battleSupport,
                battleMain = battleMainWeights?.let { LearnedBattleMainPolicy(it) } ?: baseline.battleMain
            )
        }
}

internal data class PolicyBindingMetadata(
    val name: String,
    val mode: String,
    val path: Path?,
    val sha256: String?,
    val provenance: Map<String, String>
)

internal data class InteractionRunMetadata(
    val samples: Int,
    val players: Int,
    val rounds: String,
    val grovePattern: String,
    val groveSeed: Long,
    val mechanicalSeedStart: Long,
    val mechanicalSeedEnd: Long,
    val strategySeedStart: Long,
    val strategySeedEnd: Long,
    val plantBaseline: String,
    val plantBaselineSha256: String?,
    val roundOverride: String,
    val roundOverrideSha256: String?,
    val policies: List<PolicyBindingMetadata>
)

internal fun interactionMetadata(o: PolicyInteractionOptions): InteractionRunMetadata {
    fun binding(name: String, mode: String, path: Path): PolicyBindingMetadata =
        if (mode == "learned") PolicyBindingMetadata(name, mode, path, sha256(path), weightMetadata(path))
        else PolicyBindingMetadata(name, mode, null, null, emptyMap())

    return InteractionRunMetadata(
        samples = o.games,
        players = o.players,
        rounds = o.roundLabel,
        grovePattern = o.grovePattern ?: "FIRST_GAME",
        groveSeed = o.groveSeed,
        mechanicalSeedStart = o.seed,
        mechanicalSeedEnd = o.seed + o.games - 1,
        strategySeedStart = o.strategySeed,
        strategySeedEnd = o.strategySeed + o.games - 1,
        plantBaseline = o.plantOverrides?.toString() ?: "canonical",
        plantBaselineSha256 = o.plantOverrides?.let(::sha256),
        roundOverride = o.roundOverrides?.toString() ?: "canonical",
        roundOverrideSha256 = o.roundOverrides?.let(::sha256),
        policies = listOf(
            binding("Buy", o.buyPolicy, o.buyWeights),
            binding("Cultivation Main", o.cultivationMainPolicy, o.cultivationMainWeights),
            binding("Cultivation Support", o.cultivationSupportPolicy, o.cultivationSupportWeights),
            binding("Wisp", o.wispPolicy, o.wispWeights),
            binding("Plant Effect", o.plantEffectPolicy, o.plantEffectWeights),
            binding("Battle Support", o.battleSupportPolicy, o.battleSupportWeights),
            binding("Battle Main", o.battleMainPolicy, o.battleMainWeights)
        )
    )
}

private fun sha256(path: Path): String {
    val digest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun weightMetadata(path: Path): Map<String, String> {
    val interesting = setOf(
        "formatVersion", "policy", "trainingStatus", "trainedRoundPattern", "trainedGrove",
        "trainingGenerations", "trainingGamesPerPolicy", "trainingPopulation", "trainingPlayerCount",
        "trainingEvolutionSeed", "trainingMechanicalSeedStart", "trainingStrategySeedStart", "trainingFitness",
        "cardCatalogFingerprint", "effectCatalogFingerprint", "wispCatalogFingerprint"
    )
    return Files.readAllLines(path)
        .asSequence()
        .filter { it.isNotBlank() && !it.trimStart().startsWith("#") && '=' in it }
        .map { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
        .filter { it.first in interesting }
        .toMap(linkedMapOf())
}

internal fun renderPolicyConfiguration(m: InteractionRunMetadata): String = buildString {
    appendLine("RUN CONTROL")
    appendLine("  samples=${m.samples}; players=${m.players}; rounds=${m.rounds}")
    appendLine("  Grove=${m.grovePattern}; groveSeed=${m.groveSeed}")
    appendLine("  mechanical seeds=${m.mechanicalSeedStart}..${m.mechanicalSeedEnd}")
    appendLine("  strategy seeds=${m.strategySeedStart}..${m.strategySeedEnd}")
    appendLine("  Plant baseline=${m.plantBaseline}; sha256=${m.plantBaselineSha256 ?: "canonical"}")
    appendLine("  Round override=${m.roundOverride}; sha256=${m.roundOverrideSha256 ?: "canonical"}")
    appendLine("POLICY CONFIGURATION")
    m.policies.forEach { p ->
        append("  ${p.name.padEnd(21)} ${p.mode.padEnd(7)} ${p.path ?: "-"}")
        p.sha256?.let { append(" sha256=$it") }
        appendLine()
        if (p.provenance.isNotEmpty()) {
            appendLine("    provenance=" + p.provenance.entries.joinToString("; ") { "${it.key}=${it.value}" })
        }
    }
}.trimEnd()

internal fun renderMachineConfig(m: InteractionRunMetadata): String = buildString {
    appendLine("formatVersion=1")
    appendLine("samples=${m.samples}")
    appendLine("players=${m.players}")
    appendLine("rounds=${m.rounds}")
    appendLine("grovePattern=${m.grovePattern}")
    appendLine("groveSeed=${m.groveSeed}")
    appendLine("mechanicalSeedStart=${m.mechanicalSeedStart}")
    appendLine("mechanicalSeedEnd=${m.mechanicalSeedEnd}")
    appendLine("strategySeedStart=${m.strategySeedStart}")
    appendLine("strategySeedEnd=${m.strategySeedEnd}")
    appendLine("plantBaseline=${m.plantBaseline}")
    appendLine("plantBaselineSha256=${m.plantBaselineSha256 ?: "canonical"}")
    appendLine("roundOverride=${m.roundOverride}")
    appendLine("roundOverrideSha256=${m.roundOverrideSha256 ?: "canonical"}")
    m.policies.forEach { p ->
        val key = p.name.lowercase().replace(" ", "-")
        appendLine("policy.$key.mode=${p.mode}")
        appendLine("policy.$key.weights=${p.path ?: "-"}")
        appendLine("policy.$key.sha256=${p.sha256 ?: "-"}")
        p.provenance.forEach { (metaKey, value) -> appendLine("policy.$key.$metaKey=$value") }
    }
}

internal data class MulchOutcomeBucket(
    var playerGames: Long = 0,
    var winShare: Double = 0.0,
    var totalVp: Long = 0,
    var battleVp: Long = 0,
    var wounds: Long = 0,
    var finalDicePower: Long = 0,
    var unusedMulch: Long = 0
) {
    fun add(
        playerWinShare: Double,
        playerTotalVp: Int,
        playerBattleVp: Int,
        playerWounds: Int,
        playerFinalDicePower: Int,
        playerUnusedMulch: Int
    ) {
        playerGames++
        winShare += playerWinShare
        totalVp += playerTotalVp
        battleVp += playerBattleVp
        wounds += playerWounds
        finalDicePower += playerFinalDicePower
        unusedMulch += playerUnusedMulch
    }
}

internal fun mulchBucket(count: Int): Int = count.coerceAtMost(5)

internal class InteractionAccumulator {
    var win = 0.0
    var totalVp = 0L; var plantVp = 0L; var battleVp = 0L; var wispVp = 0L; var otherVp = 0L; var wounds = 0L
    var plantPurchases = 0L; var diePurchases = 0L
    val plantPurchaseByName = sortedMapOf<String, Long>(); val diePurchaseByName = sortedMapOf<String, Long>()
    var finalDice = 0L; var finalDicePower = 0L

    var cultivationRounds = 0L; var battleRounds = 0L
    val cultivationActions = linkedMapOf<MainActionKind, Long>()
    val battleFirstActions = linkedMapOf<MainActionKind, Long>()
    val battleFinalActions = linkedMapOf<MainActionKind, Long>()
    val battleSunlightActions = linkedMapOf<MainActionKind, Long>()
    val cultivationPlantMainByName = sortedMapOf<String, Long>()
    val battlePlantMainByName = sortedMapOf<String, Long>()

    val cultivationSupportActions = linkedMapOf<SupportActionKind, Long>()
    val supportActions = linkedMapOf<SupportActionKind, Long>()
    var cultivationSupportPasses = 0L
    var cultivationSupportWindows = 0L
    val cultivationSupportOpportunities = sortedMapOf<String, Long>()
    val cultivationSupportUsesByFamily = sortedMapOf<String, Long>()
    var waterRerollDelta = 0L; var waterRerollsMeasured = 0L
    var refreshedPlants = 0L; var refreshedButterflies = 0L

    val cultivationRoundEffectOpportunities = sortedMapOf<String, Long>()
    val cultivationRoundEffectUses = sortedMapOf<String, Long>()
    val battleRoundEffectOpportunities = sortedMapOf<String, Long>()
    val battleRoundEffectUses = sortedMapOf<String, Long>()

    var wispPolicyDecisions = 0L
    var wispPolicyHolds = 0L
    val wispDecisionsByPhase = linkedMapOf<ChroniclePhase, Long>()
    val wispHoldsByPhase = linkedMapOf<ChroniclePhase, Long>()
    val wispOpportunities = sortedMapOf<String, Long>()
    val wispOffers = sortedMapOf<String, Long>()
    val wispHoldWindowsByName = sortedMapOf<String, Long>()
    val wispActualPlays = sortedMapOf<String, Long>()
    val wispCultivationPlays = sortedMapOf<String, Long>()
    val wispBattlePlays = sortedMapOf<String, Long>()
    val finalRetainedWisps = sortedMapOf<String, Long>()
    var wispGains = 0L

    var plantEffectDecisions = 0L
    var plantEffectDisagreements = 0L
    val plantEffectByCard = sortedMapOf<String, Long>()
    val plantEffectByPhase = linkedMapOf<ChroniclePhase, Long>()
    val plantEffectChoiceDistribution = sortedMapOf<String, Long>()
    val cultivationPlantActivationsByName = sortedMapOf<String, Long>()
    val battlePlantActivationsByName = sortedMapOf<String, Long>()
    val battlePlantDecisionOpportunitiesByStage = sortedMapOf<String, Long>()
    val battlePlantDecisionSelectionsByStage = sortedMapOf<String, Long>()
    val battlePlantOpportunitiesByStageCard = sortedMapOf<String, Long>()
    val battlePlantUsesByStageCard = sortedMapOf<String, Long>()
    val battlePlantSubstitutionsByStageCard = sortedMapOf<String, Long>()

    var battlePlantActivations = 0L; var cultivationPlantActivations = 0L
    var strikeWins = 0L; var winnerDecisiveRows = 0L; var winnerDecisiveContributions = 0L; var woundDecisiveContributions = 0L
    var sunlightGainOpp = 0L; var sunlightGainUses = 0L
    var sunlightGained = 0L; var sunlightSpent = 0L; var finalSunlight = 0L; var sunlightOpportunities = 0L
    var sunlightUses = 0L; var sunlightExtraMains = 0L; var sunlightImmediate = 0L; var sunlightDecisive = 0L; var sunlightWoundDecisive = 0L

    val tokenUses = SharedTokenResource.entries.associateWith { 0L }.toMutableMap()
    val tokenGains = SharedTokenResource.entries.associateWith { 0L }.toMutableMap()
    val tokenFinalGrove = SharedTokenResource.entries.associateWith { 0L }.toMutableMap()
    val tokenFinalHeldByPlayers = SharedTokenResource.entries.associateWith { 0L }.toMutableMap()
    val tokenZeroGames = SharedTokenResource.entries.associateWith { 0L }.toMutableMap()
    var finalWater = 0L; var finalMulch = 0L; var finalWorm = 0L; var finalBee = 0L; var finalButterfly = 0L; var finalWisp = 0L

    // Per-player-game Mulch telemetry. Bucket 5 represents 5+.
    val roundMulchOutcomeByCount = sortedMapOf<Int, MulchOutcomeBucket>()
    val battleMulchOutcomeByCount = sortedMapOf<Int, MulchOutcomeBucket>()

    fun add(game: CompletedInteractionGame, seat: Int) {
        val p = game.summary.players.single { it.seat == seat }
        win += p.winShare; totalVp += p.totalVp; plantVp += p.plantVp; battleVp += p.battleStrikeVp; wispVp += p.unplayedWispVp
        otherVp += (p.totalVp - p.plantVp - p.battleStrikeVp - p.unplayedWispVp)
        wounds += p.woundsTaken; finalDice += p.finalDiceCount; finalDicePower += p.finalDicePower
        sunlightGained += p.sunlightGained; sunlightSpent += p.sunlightSpent; finalSunlight += p.finalSunlightCount
        sunlightOpportunities += p.sunlightSupportOpportunities
        sunlightUses += p.sunlightSupportUses; sunlightExtraMains += p.sunlightExtraMainActions; sunlightImmediate += p.sunlightImmediateStrikeContributions
        sunlightDecisive += p.sunlightWinnerDecisiveContributions; sunlightWoundDecisive += p.sunlightWoundDecisiveContributions
        finalWisp += p.finalWispCount
        wispGains += p.rollRewardWispsGained

        val roundMulchSelections = game.entries.filterIsInstance<GameEntry.RoundEffectChoice>()
            .filter { it.playerId == p.playerId }
            .count { e ->
            if (e.phase != ChroniclePhase.CULTIVATION) return@count false
            when (e.selectedMainAction) {
                MainActionKind.ROUND_EFFECT_1 -> e.firstExecutable && e.firstEffect == GameEffect.MULCH_DIE_FROM_HAND
                MainActionKind.ROUND_EFFECT_2 -> e.secondExecutable && e.secondEffect == GameEffect.MULCH_DIE_FROM_HAND
                else -> false
            }
        }
        val battleMulchUses = game.entries.filterIsInstance<GameEntry.SupportAction>()
            .filter { it.playerId == p.playerId }
            .count { e ->
                e.phase == ChroniclePhase.BATTLE && e.action == SupportActionKind.MULCH
            }
        val finalPlayerSummary = game.entries.filterIsInstance<GameEntry.RoundCompleted>()
            .lastOrNull()?.playerSummaries?.firstOrNull { it.playerId == p.playerId }
        val unusedMulch = finalPlayerSummary?.mulchDice?.size ?: 0
        fun recordMulchOutcome(target: MutableMap<Int, MulchOutcomeBucket>, count: Int) {
            target.getOrPut(mulchBucket(count)) { MulchOutcomeBucket() }.add(
                playerWinShare = p.winShare,
                playerTotalVp = p.totalVp,
                playerBattleVp = p.battleStrikeVp,
                playerWounds = p.woundsTaken,
                playerFinalDicePower = p.finalDicePower,
                playerUnusedMulch = unusedMulch
            )
        }
        recordMulchOutcome(roundMulchOutcomeByCount, roundMulchSelections)
        recordMulchOutcome(battleMulchOutcomeByCount, battleMulchUses)

        game.entries.filterIsInstance<GameEntry.RoundRevealed>().forEach { e ->
            if (e.cardType == dugsolutions.leaf.v35.round.domain.RoundCardType.CULTIVATION) cultivationRounds++ else battleRounds++
        }

        game.entries.filterIsInstance<GameEntry.Purchase>().filter { it.playerId == p.playerId }.forEach { e ->
            when(e.kind) {
                PurchaseKind.PLANT -> { plantPurchases++; plantPurchaseByName[e.itemName] = (plantPurchaseByName[e.itemName] ?: 0) + 1 }
                PurchaseKind.DIE -> { diePurchases++; diePurchaseByName[e.itemName] = (diePurchaseByName[e.itemName] ?: 0) + 1 }
            }
        }

        game.entries.filterIsInstance<GameEntry.MainAction>().filter { it.playerId == p.playerId }.forEach { e ->
            if (e.phase == ChroniclePhase.CULTIVATION) {
                cultivationActions[e.action] = (cultivationActions[e.action] ?: 0) + 1
            } else {
                val target = when (e.battleStage) {
                    BattleMainStage.FIRST -> battleFirstActions
                    BattleMainStage.FINAL -> battleFinalActions
                    BattleMainStage.SUNLIGHT -> battleSunlightActions
                    null -> null
                }
                if (target != null) target[e.action] = (target[e.action] ?: 0) + 1
            }
        }

        game.entries.filterIsInstance<GameEntry.RoundEffectChoice>().filter { it.playerId == p.playerId }.forEach { e ->
            val opp = if (e.phase == ChroniclePhase.CULTIVATION) cultivationRoundEffectOpportunities else battleRoundEffectOpportunities
            val use = if (e.phase == ChroniclePhase.CULTIVATION) cultivationRoundEffectUses else battleRoundEffectUses
            if (e.firstExecutable) opp[e.firstEffect.name] = (opp[e.firstEffect.name] ?: 0) + 1
            if (e.secondExecutable) opp[e.secondEffect.name] = (opp[e.secondEffect.name] ?: 0) + 1
            when (e.selectedMainAction) {
                MainActionKind.ROUND_EFFECT_1 -> if (e.firstExecutable) use[e.firstEffect.name] = (use[e.firstEffect.name] ?: 0) + 1
                MainActionKind.ROUND_EFFECT_2 -> if (e.secondExecutable) use[e.secondEffect.name] = (use[e.secondEffect.name] ?: 0) + 1
                MainActionKind.ACTIVATE_PLANT -> e.selectedPlantCardName?.let { name ->
                    val map = if (e.phase == ChroniclePhase.CULTIVATION) cultivationPlantMainByName else battlePlantMainByName
                    map[name] = (map[name] ?: 0) + 1
                }
                else -> Unit
            }
            if (e.phase == ChroniclePhase.BATTLE && e.legalPlantCardNames.isNotEmpty()) {
                val stage = e.battleStage?.name ?: "UNKNOWN"
                battlePlantDecisionOpportunitiesByStage[stage] = (battlePlantDecisionOpportunitiesByStage[stage] ?: 0) + 1
                if (e.selectedMainAction == MainActionKind.ACTIVATE_PLANT) {
                    battlePlantDecisionSelectionsByStage[stage] = (battlePlantDecisionSelectionsByStage[stage] ?: 0) + 1
                }
                val alternative = when (e.selectedMainAction) {
                    MainActionKind.DRAW -> "DRAW"
                    MainActionKind.ACTIVATE_PLANT -> "PLANT:${e.selectedPlantCardName ?: "UNKNOWN"}"
                    MainActionKind.ROUND_EFFECT_1 -> "ROUND:SLOT_1:${e.firstEffect.name}"
                    MainActionKind.ROUND_EFFECT_2 -> "ROUND:SLOT_2:${e.secondEffect.name}"
                    null -> "NONE"
                }
                e.legalPlantCardNames.distinct().forEach { card ->
                    val key = "$stage|$card"
                    battlePlantOpportunitiesByStageCard[key] = (battlePlantOpportunitiesByStageCard[key] ?: 0) + 1
                    if (e.selectedMainAction == MainActionKind.ACTIVATE_PLANT && e.selectedPlantCardName == card) {
                        battlePlantUsesByStageCard[key] = (battlePlantUsesByStageCard[key] ?: 0) + 1
                    } else {
                        val subKey = "$stage|$card|$alternative"
                        battlePlantSubstitutionsByStageCard[subKey] = (battlePlantSubstitutionsByStageCard[subKey] ?: 0) + 1
                    }
                }
            }
            if (e.phase == ChroniclePhase.CULTIVATION) {
                val firstSun = e.firstEffect == GameEffect.GAIN_SUNLIGHT_TOKEN && e.firstExecutable
                val secondSun = e.secondEffect == GameEffect.GAIN_SUNLIGHT_TOKEN && e.secondExecutable
                if (firstSun || secondSun) {
                    sunlightGainOpp++
                    if ((firstSun && e.selectedMainAction == MainActionKind.ROUND_EFFECT_1) || (secondSun && e.selectedMainAction == MainActionKind.ROUND_EFFECT_2)) sunlightGainUses++
                }
            }
        }

        val cultivationSupportDecisions = game.entries.filterIsInstance<GameEntry.CultivationSupportDecision>().filter { it.playerId == p.playerId }
        cultivationSupportDecisions.forEach { e ->
            cultivationSupportWindows++
            if (e.selectedActionId == null) cultivationSupportPasses++
            e.legalActionIds.map(::cultivationSupportFamily).distinct().forEach { family ->
                cultivationSupportOpportunities[family] = (cultivationSupportOpportunities[family] ?: 0) + 1
            }
            e.selectedActionId?.let { id ->
                val family = cultivationSupportFamily(id)
                cultivationSupportUsesByFamily[family] = (cultivationSupportUsesByFamily[family] ?: 0) + 1
                if (family == "WATER_REFRESH") {
                    refreshedPlants += e.faceDownPlants
                    refreshedButterflies += e.spentButterflies
                }
            }
        }
        game.entries.filterIsInstance<GameEntry.SupportAction>().filter { it.playerId == p.playerId }.forEach { e ->
            val map = if (e.phase == ChroniclePhase.CULTIVATION) cultivationSupportActions else supportActions
            map[e.action] = (map[e.action] ?: 0) + 1
        }
        game.entries.filterIsInstance<GameEntry.SupportAction>()
            .filter { it.playerId == p.playerId && it.phase == ChroniclePhase.CULTIVATION && it.action == SupportActionKind.WATER_REROLL }
            .forEach { support ->
                val decision = cultivationSupportDecisions.lastOrNull { it.sequence < support.sequence && cultivationSupportFamily(it.selectedActionId ?: "") == "WATER_REROLL" }
                val original = decision?.selectedActionId?.substringAfterLast('@')?.toIntOrNull()
                val roll = game.entries.filterIsInstance<GameEntry.DieRolled>().firstOrNull { it.playerId == p.playerId && it.sequence > support.sequence && it.hierarchyDepth > support.hierarchyDepth }
                if (original != null && roll != null) { waterRerollDelta += (roll.value - original); waterRerollsMeasured++ }
            }

        game.entries.filterIsInstance<GameEntry.WispPlayDecision>().filter { it.playerId == p.playerId }.forEach { e ->
            wispPolicyDecisions++
            wispDecisionsByPhase[e.phase] = (wispDecisionsByPhase[e.phase] ?: 0) + 1
            if (e.selectedWispName == null) {
                wispPolicyHolds++
                wispHoldsByPhase[e.phase] = (wispHoldsByPhase[e.phase] ?: 0) + 1
                e.legalWispNames.distinct().forEach { name -> wispHoldWindowsByName[name] = (wispHoldWindowsByName[name] ?: 0) + 1 }
            }
            e.legalWispNames.distinct().forEach { name -> wispOpportunities[name] = (wispOpportunities[name] ?: 0) + 1 }
            e.selectedWispName?.let { name -> wispOffers[name] = (wispOffers[name] ?: 0) + 1 }
        }
        game.entries.filterIsInstance<GameEntry.WispAcquired>().filter { it.playerId == p.playerId }.forEach { wispGains++ }
        game.entries.filterIsInstance<GameEntry.EffectResolved>().filter { it.playerId == p.playerId }.forEach { e ->
            when (e.sourceKind) {
                EffectSourceKind.PLANT -> {
                    if (e.phase == ChroniclePhase.CULTIVATION) {
                        cultivationPlantActivations++
                        cultivationPlantActivationsByName[e.sourceName] = (cultivationPlantActivationsByName[e.sourceName] ?: 0) + 1
                    } else {
                        battlePlantActivations++
                        battlePlantActivationsByName[e.sourceName] = (battlePlantActivationsByName[e.sourceName] ?: 0) + 1
                    }
                }
                EffectSourceKind.WISP -> {
                    wispActualPlays[e.sourceName] = (wispActualPlays[e.sourceName] ?: 0) + 1
                    val phaseMap = if (e.phase == ChroniclePhase.CULTIVATION) wispCultivationPlays else wispBattlePlays
                    phaseMap[e.sourceName] = (phaseMap[e.sourceName] ?: 0) + 1
                }
                EffectSourceKind.ROUND -> Unit
            }
        }
        game.entries.filterIsInstance<GameEntry.FinalScore>().filter { it.playerId == p.playerId }.forEach { e ->
            e.unplayedWispNames.forEach { name -> finalRetainedWisps[name] = (finalRetainedWisps[name] ?: 0) + 1 }
        }

        game.entries.filterIsInstance<GameEntry.PlantEffectDecision>().filter { it.playerId == p.playerId }.forEach { e ->
            plantEffectDecisions++
            plantEffectByCard[e.plantId] = (plantEffectByCard[e.plantId] ?: 0) + 1
            plantEffectByPhase[e.phase] = (plantEffectByPhase[e.phase] ?: 0) + 1
            if (e.referenceChoiceId != null && e.referenceChoiceId != e.selectedChoiceId) plantEffectDisagreements++
            val key = "${e.phase.name}|${e.plantId}|${e.decisionKind}|${e.selectedChoiceId}"
            plantEffectChoiceDistribution[key] = (plantEffectChoiceDistribution[key] ?: 0) + 1
        }

        game.entries.filterIsInstance<GameEntry.StrikeResolved>().forEach { e ->
            if (p.playerId in e.winnerIds) strikeWins++
            val own = e.contributionLedger?.contributions.orEmpty().filter { it.playerId == p.playerId }
            val winnerDecisive = own.count { it.individuallyWinnerDecisive }
            winnerDecisiveContributions += winnerDecisive
            woundDecisiveContributions += own.count { it.individuallyWoundDecisive }
            if (winnerDecisive > 0) winnerDecisiveRows++
        }

        game.summary.sharedTokenEconomy.forEach { e ->
            tokenUses[e.resource] = (tokenUses[e.resource] ?: 0) + e.spendsOrUses
            tokenGains[e.resource] = (tokenGains[e.resource] ?: 0) + e.successfulGains
            tokenFinalGrove[e.resource] = (tokenFinalGrove[e.resource] ?: 0) + e.finalGroveSupply
            tokenFinalHeldByPlayers[e.resource] = (tokenFinalHeldByPlayers[e.resource] ?: 0) + e.finalHeldByPlayers
            if (e.reachedZero) tokenZeroGames[e.resource] = (tokenZeroGames[e.resource] ?: 0) + 1
        }

        game.entries.filterIsInstance<GameEntry.RoundCompleted>().lastOrNull()?.playerSummaries?.firstOrNull { it.playerId == p.playerId }?.let { end ->
            finalWater += end.waterCount
            finalMulch += end.mulchDice.size
            finalWorm += end.wormCount
            finalBee += end.beeCount
            finalButterfly += end.butterflies.size
        }
    }
}

private fun cultivationSupportFamily(id: String): String = when {
    ":WATER_REROLL:" in id -> "WATER_REROLL"
    id.endsWith(":WATER_REFRESH") -> "WATER_REFRESH"
    ":MULCH:" in id -> "MULCH"
    ":WORM_FLIP:" in id -> "WORM_FLIP"
    ":BUTTERFLY:" in id -> "BUTTERFLY"
    ":WISP:" in id -> "WISP"
    id.isBlank() -> "PASS"
    else -> id.substringAfter("CULT_SUPPORT:", id).substringBefore(':')
}

internal fun printReport(a: InteractionAccumulator, n: Int, verbosePlantTargeting: Boolean = false) {
    fun avg(v: Long) = v.toDouble()/n
    fun fmt(v: Double) = "%.3f".format(v)
    fun pct(v: Double) = "%.2f%%".format(v*100)
    fun takeRate(use: Long, opp: Long) = pct(if (opp == 0L) 0.0 else use.toDouble()/opp)
    fun mainTotal(map: Map<MainActionKind, Long>) = map.values.sum()

    println("OUTCOME")
    println("  win share=${pct(a.win/n)} total VP=${fmt(avg(a.totalVp))} Plant VP=${fmt(avg(a.plantVp))} Battle VP=${fmt(avg(a.battleVp))} Wisp VP=${fmt(avg(a.wispVp))} other VP=${fmt(avg(a.otherVp))} wounds=${fmt(avg(a.wounds))}")
    println("  final dice/game=${fmt(avg(a.finalDice))}; final dice power/game=${fmt(avg(a.finalDicePower))}")
    println()

    println("ECONOMY")
    println("  Plant purchases/game=${fmt(avg(a.plantPurchases))}; dice purchases/game=${fmt(avg(a.diePurchases))}")
    println("  Plant purchase distribution:")
    a.plantPurchaseByName.toList().sortedByDescending { it.second }.forEach { (k,v) -> println("    $k: total=$v per-game=${fmt(v.toDouble()/n)}") }
    println("  Die purchase distribution:")
    a.diePurchaseByName.toList().sortedByDescending { it.second }.forEach { (k,v) -> println("    $k: total=$v per-game=${fmt(v.toDouble()/n)}") }
    println("  affected-player final holdings/game: Water=${fmt(avg(a.finalWater))} Mulch=${fmt(avg(a.finalMulch))} Worm=${fmt(avg(a.finalWorm))} Bee=${fmt(avg(a.finalBee))} Butterfly=${fmt(avg(a.finalButterfly))} Sunlight=${fmt(avg(a.finalSunlight))} Wisp=${fmt(avg(a.finalWisp))}")
    println("  Sunlight affected-player flow/game: gained=${fmt(avg(a.sunlightGained))} spent=${fmt(avg(a.sunlightSpent))} final=${fmt(avg(a.finalSunlight))}")
    println("  Whole-table finite-token economy:")
    SharedTokenResource.entries.forEach { r ->
        println("    ${r.name}: gains/game=${fmt((a.tokenGains[r]?:0).toDouble()/n)} uses/game=${fmt((a.tokenUses[r]?:0).toDouble()/n)} final-grove=${fmt((a.tokenFinalGrove[r]?:0).toDouble()/n)} final-held=${fmt((a.tokenFinalHeldByPlayers[r]?:0).toDouble()/n)} reached-zero=${a.tokenZeroGames[r]?:0}/$n")
    }
    println()

    println("CULTIVATION MAIN")
    val cultTotal = mainTotal(a.cultivationActions)
    println("  total Main actions/game=${fmt(cultTotal.toDouble()/n)}")
    MainActionKind.entries.forEach { k ->
        val count = a.cultivationActions[k] ?: 0
        println("  ${k.name}: ${fmt(count.toDouble()/n)}/game rate=${takeRate(count, cultTotal)}")
    }
    println("  Plant activations/game=${fmt(avg(a.cultivationPlantActivations))}")
    if (a.cultivationPlantActivationsByName.isNotEmpty()) {
        println("  Plant activations by card:")
        a.cultivationPlantActivationsByName.toList().sortedByDescending { it.second }.forEach { (name,count) -> println("    $name: ${fmt(count.toDouble()/n)}/game") }
    }
    println("  Round-effect opportunities / uses:")
    (a.cultivationRoundEffectOpportunities.keys + a.cultivationRoundEffectUses.keys).toSortedSet().forEach { effect ->
        val o = a.cultivationRoundEffectOpportunities[effect] ?: 0
        val u = a.cultivationRoundEffectUses[effect] ?: 0
        println("    $effect: opportunities=$o uses=$u take-rate=${takeRate(u,o)} uses/game=${fmt(u.toDouble()/n)}")
    }
    println("  Gain Sunlight take rate=${takeRate(a.sunlightGainUses,a.sunlightGainOpp)} (${a.sunlightGainUses}/${a.sunlightGainOpp} executable opportunities)")
    println()

    println("CULTIVATION SUPPORT")
    val cultSupportUses = a.cultivationSupportActions.values.sum()
    println("  decision windows/game=${fmt(avg(a.cultivationSupportWindows))}; PASS/DONE=${fmt(avg(a.cultivationSupportPasses))}/game (${takeRate(a.cultivationSupportPasses,a.cultivationSupportWindows)})")
    println("  Supports/game=${fmt(cultSupportUses.toDouble()/n)}; Supports/Cultivation round=${fmt(if(a.cultivationRounds==0L)0.0 else cultSupportUses.toDouble()/a.cultivationRounds)}")
    val families = (a.cultivationSupportOpportunities.keys + a.cultivationSupportUsesByFamily.keys).toSortedSet()
    families.forEach { family ->
        val o = a.cultivationSupportOpportunities[family] ?: 0
        val u = a.cultivationSupportUsesByFamily[family] ?: 0
        println("  $family: opportunities=$o uses=$u take-rate=${takeRate(u,o)} uses/game=${fmt(u.toDouble()/n)}")
    }
    println("  action execution/game:")
    SupportActionKind.entries.forEach { k -> println("    ${k.name}: ${fmt((a.cultivationSupportActions[k]?:0).toDouble()/n)}") }
    println("  Water reroll realized delta/use=${fmt(if(a.waterRerollsMeasured==0L)0.0 else a.waterRerollDelta.toDouble()/a.waterRerollsMeasured)} measured=${a.waterRerollsMeasured}")
    println("  Water refresh visible recovery/game: Plants=${fmt(a.refreshedPlants.toDouble()/n)} Butterflies=${fmt(a.refreshedButterflies.toDouble()/n)}")
    println()

    println("WISP PLAY")
    println("  gains/game=${fmt(avg(a.wispGains))}; decisions/game=${fmt(avg(a.wispPolicyDecisions))}; HOLD=${fmt(avg(a.wispPolicyHolds))}/game rate=${takeRate(a.wispPolicyHolds,a.wispPolicyDecisions)}; retained/game=${fmt(avg(a.finalWisp))}; retained VP/game=${fmt(avg(a.wispVp))}")
    ChroniclePhase.entries.forEach { phase ->
        val d = a.wispDecisionsByPhase[phase] ?: 0; val h = a.wispHoldsByPhase[phase] ?: 0
        println("  ${phase.name}: decisions=${fmt(d.toDouble()/n)}/game HOLD-rate=${takeRate(h,d)}")
    }
    (a.wispOpportunities.keys + a.wispOffers.keys + a.wispActualPlays.keys + a.finalRetainedWisps.keys).toSortedSet().forEach { name ->
        val o=a.wispOpportunities[name]?:0; val offered=a.wispOffers[name]?:0; val played=a.wispActualPlays[name]?:0
        println("  $name: opportunities=$o offered=$offered offer-rate=${takeRate(offered,o)} HOLD-windows=${a.wispHoldWindowsByName[name]?:0} actual-plays=$played Cultivation=${a.wispCultivationPlays[name]?:0} Battle=${a.wispBattlePlays[name]?:0} final-retained=${a.finalRetainedWisps[name]?:0}")
    }
    println()

    println("PLANT EXECUTION")
    println("  targeting decisions/game=${fmt(avg(a.plantEffectDecisions))}; Human-reference disagreement=${takeRate(a.plantEffectDisagreements,a.plantEffectDecisions)}")
    ChroniclePhase.entries.forEach { phase -> println("  ${phase.name} targeting decisions/game=${fmt((a.plantEffectByPhase[phase]?:0).toDouble()/n)}") }
    a.plantEffectByCard.toList().sortedByDescending { it.second }.forEach { (card,count) -> println("  $card: decisions=${count} per-game=${fmt(count.toDouble()/n)}") }
    if (a.plantEffectChoiceDistribution.isNotEmpty()) {
        if (verbosePlantTargeting) {
            println("  target/branch distribution [phase|card|decision|selected]:")
            a.plantEffectChoiceDistribution.toList().sortedWith(compareByDescending<Pair<String,Long>> { it.second }.thenBy { it.first }).forEach { (key,count) -> println("    $key: $count") }
        } else {
            println("  detailed target/branch distribution suppressed (${a.plantEffectChoiceDistribution.size} distinct states; use --verbose-plant-targeting to print)")
        }
    }
    println()

    println("BATTLE MAIN")
    fun printMainStage(label:String,map:Map<MainActionKind,Long>) {
        val total=mainTotal(map)
        println("  $label total/game=${fmt(total.toDouble()/n)}")
        MainActionKind.entries.forEach { k -> val c=map[k]?:0; println("    ${k.name}: ${fmt(c.toDouble()/n)}/game rate=${takeRate(c,total)}") }
    }
    printMainStage("FIRST", a.battleFirstActions)
    printMainStage("FINAL", a.battleFinalActions)
    printMainStage("SUNLIGHT-FUNDED", a.battleSunlightActions)
    if (a.battlePlantActivationsByName.isNotEmpty()) {
        println("  Battle Plant activations by card:")
        a.battlePlantActivationsByName.toList().sortedByDescending { it.second }.forEach { (name,count) -> println("    $name: ${fmt(count.toDouble()/n)}/game") }
    }
    println("  Plant-available Main decisions by stage:")
    BattleMainStage.entries.forEach { stage ->
        val key = stage.name
        val o = a.battlePlantDecisionOpportunitiesByStage[key] ?: 0
        val u = a.battlePlantDecisionSelectionsByStage[key] ?: 0
        println("    $key: opportunities=$o Plant-selected=$u take-rate=${takeRate(u,o)}")
    }
    if (a.battlePlantOpportunitiesByStageCard.isNotEmpty()) {
        println("  Battle Plant opportunity-normalized use by card and stage:")
        val cards = a.battlePlantOpportunitiesByStageCard.keys.map { it.substringAfter('|') }.toSortedSet()
        cards.forEach { card ->
            var totalO = 0L
            var totalU = 0L
            val stageParts = BattleMainStage.entries.map { stage ->
                val key = "${stage.name}|$card"
                val o = a.battlePlantOpportunitiesByStageCard[key] ?: 0
                val u = a.battlePlantUsesByStageCard[key] ?: 0
                totalO += o; totalU += u
                "${stage.name}=$u/$o(${takeRate(u,o)})"
            }
            println("    $card: uses=$totalU opportunities=$totalO rate=${takeRate(totalU,totalO)}; ${stageParts.joinToString(" ")}")
            val substitutions = a.battlePlantSubstitutionsByStageCard
                .filterKeys { it.substringAfter('|').substringBefore('|') == card }
                .entries.sortedByDescending { it.value }.take(6)
            if (substitutions.isNotEmpty()) {
                println("      when not chosen, top alternatives:")
                substitutions.forEach { (key,count) ->
                    val parts = key.split('|', limit = 3)
                    println("        ${parts[0]} -> ${parts[2]}: $count")
                }
            }
        }
    }
    println("  Battle Round-effect opportunities / uses:")
    (a.battleRoundEffectOpportunities.keys + a.battleRoundEffectUses.keys).toSortedSet().forEach { effect ->
        val o=a.battleRoundEffectOpportunities[effect]?:0; val u=a.battleRoundEffectUses[effect]?:0
        println("    $effect: opportunities=$o uses=$u take-rate=${takeRate(u,o)} uses/game=${fmt(u.toDouble()/n)}")
    }
    println()

    println("BATTLE SUPPORT")
    val battleSupports = a.supportActions.values.sum()
    println("  Supports before FINAL/game=${fmt(battleSupports.toDouble()/n)}; Supports/Battle=${fmt(if(a.battleRounds==0L)0.0 else battleSupports.toDouble()/a.battleRounds)}")
    println("  Support action distribution/game:")
    SupportActionKind.entries.forEach { k -> println("    ${k.name}: ${fmt((a.supportActions[k]?:0).toDouble()/n)}") }
    println("  Sunlight opportunities/game=${fmt(avg(a.sunlightOpportunities))}; uses/game=${fmt(avg(a.sunlightUses))}; use-rate=${takeRate(a.sunlightUses,a.sunlightOpportunities)}")
    println("  Sunlight extra Main Actions/game=${fmt(avg(a.sunlightExtraMains))}; immediate contributions/game=${fmt(avg(a.sunlightImmediate))}; winner-decisive/game=${fmt(avg(a.sunlightDecisive))}; wound-decisive/game=${fmt(avg(a.sunlightWoundDecisive))}")
    println()

    println("BATTLE OUTCOME")
    println("  Battle Plant activations/game=${fmt(avg(a.battlePlantActivations))}; Strike wins/game=${fmt(avg(a.strikeWins))}; winner-decisive rows/game=${fmt(avg(a.winnerDecisiveRows))}")
    println("  decisive contributions/game: winner=${fmt(avg(a.winnerDecisiveContributions))}; wound=${fmt(avg(a.woundDecisiveContributions))}")
    println("  Battle VP/game=${fmt(avg(a.battleVp))}; wounds/game=${fmt(avg(a.wounds))}")
    println("  Strike flips: unavailable as a trustworthy aggregate in the current Chronicle; winner-decisive rows/contributions are reported instead.")
    println()

    println("MULCH OUTCOME BY PLAYER-GAME")
    fun printMulchBuckets(label: String, buckets: Map<Int, MulchOutcomeBucket>) {
        println("  $label")
        println("    count  player-games  win-share  total-VP  battle-VP  wounds  final-dice-power  unused-Mulch")
        (0..5).forEach { bucket ->
            val b = buckets[bucket] ?: MulchOutcomeBucket()
            val denom = b.playerGames.toDouble()
            fun bucketAvg(value: Long) = if (b.playerGames == 0L) 0.0 else value / denom
            val name = if (bucket == 5) "5+" else bucket.toString()
            println(
                "    ${name.padStart(5)}  ${b.playerGames.toString().padStart(12)}  " +
                    "${pct(if (b.playerGames == 0L) 0.0 else b.winShare / denom).padStart(9)}  " +
                    "${fmt(bucketAvg(b.totalVp)).padStart(8)}  " +
                    "${fmt(bucketAvg(b.battleVp)).padStart(9)}  " +
                    "${fmt(bucketAvg(b.wounds)).padStart(6)}  " +
                    "${fmt(bucketAvg(b.finalDicePower)).padStart(16)}  " +
                    "${fmt(bucketAvg(b.unusedMulch)).padStart(12)}"
            )
        }
        println("    threshold views:")
        (1..4).forEach { threshold ->
            val below = MulchOutcomeBucket()
            val atLeast = MulchOutcomeBucket()
            buckets.forEach { (bucket, b) ->
                val target = if (bucket < threshold) below else atLeast
                target.playerGames += b.playerGames
                target.winShare += b.winShare
                target.totalVp += b.totalVp
                target.battleVp += b.battleVp
                target.wounds += b.wounds
                target.finalDicePower += b.finalDicePower
                target.unusedMulch += b.unusedMulch
            }
            fun thresholdWin(b: MulchOutcomeBucket) = if (b.playerGames == 0L) 0.0 else b.winShare / b.playerGames
            println(
                "      <${threshold}: n=${below.playerGames} win=${pct(thresholdWin(below))}; " +
                    ">=${threshold}: n=${atLeast.playerGames} win=${pct(thresholdWin(atLeast))}"
            )
        }
    }
    printMulchBuckets("Round-card Mulch selections", a.roundMulchOutcomeByCount)
    printMulchBuckets("Battle Mulch uses", a.battleMulchOutcomeByCount)
    println("  Note: these are observational buckets, not causal estimates; stronger states may both use more Mulch and win more.")
    println()

    println("ROUND-EFFECT RESEARCH")
    val researchEffects = listOf(
        GameEffect.UPGRADE_DIE_FROM_HAND,
        GameEffect.UPGRADE_DIE_AND_USE_NOW,
        GameEffect.GAIN_WATER_TOKEN,
        GameEffect.MULCH_DIE_FROM_HAND,
        GameEffect.GAIN_SUNLIGHT_TOKEN
    )
    researchEffects.forEach { effect ->
        val key=effect.name; val o=a.cultivationRoundEffectOpportunities[key]?:0; val u=a.cultivationRoundEffectUses[key]?:0
        println("  $key: opportunities=$o uses=$u take-rate=${takeRate(u,o)}")
    }
    val draw=a.cultivationActions[MainActionKind.DRAW]?:0; val plant=a.cultivationActions[MainActionKind.ACTIVATE_PLANT]?:0
    val round=(a.cultivationActions[MainActionKind.ROUND_EFFECT_1]?:0)+(a.cultivationActions[MainActionKind.ROUND_EFFECT_2]?:0)
    println("  Cultivation action mix: Draw=${takeRate(draw,cultTotal)} Plant=${takeRate(plant,cultTotal)} RoundEffect=${takeRate(round,cultTotal)}")
}
