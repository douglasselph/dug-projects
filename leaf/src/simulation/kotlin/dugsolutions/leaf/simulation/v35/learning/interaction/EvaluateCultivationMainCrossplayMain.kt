package dugsolutions.leaf.simulation.v35.learning.interaction

import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.resolveResearchGroveForSample
import dugsolutions.leaf.simulation.v35.experiment.round.RoundExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.learning.buy.parseRoundSetup
import dugsolutions.leaf.simulation.v35.learning.cultivation.loadCultivationResearchCards
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.MainActionKind
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselineDecisionDirector
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainCatalog
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainPolicy
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainWeights
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink
import org.koin.dsl.koinApplication
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

private data class CrossplayOptions(
    val samples: Int = 2000,
    val players: Int = 4,
    val seed: Long = 8810000L,
    val strategySeed: Long = 8820000L,
    val groveSeed: Long = 8830000L,
    val rounds: String = "3/2/2",
    val plantOverrides: Path? = null,
    val roundOverrides: Path? = null,
    val weights: List<Path>,
    val labels: List<String>
) {
    val roundSetup = parseRoundSetup(rounds)

    init {
        require(samples > 0)
        require(players in 2..4)
        require(weights.size == players) {
            "--weights must provide exactly $players comma-separated files; got ${weights.size}"
        }
        require(labels.size == players) {
            "--labels must provide exactly $players comma-separated labels; got ${labels.size}"
        }
        weights.forEach { require(Files.isRegularFile(it)) { "Missing weights: $it" } }
        plantOverrides?.let { require(Files.isRegularFile(it)) { "Missing Plant overrides: $it" } }
        roundOverrides?.let { require(Files.isRegularFile(it)) { "Missing Round overrides: $it" } }
    }

    companion object {
        fun parse(args: Array<String>): CrossplayOptions {
            var samples = 2000
            var players = 4
            var seed = 8810000L
            var strategySeed = 8820000L
            var groveSeed = 8830000L
            var rounds = "3/2/2"
            var plantOverrides: Path? = null
            var roundOverrides: Path? = null
            var weights: List<Path> = emptyList()
            var labels: List<String> = emptyList()

            fun value(arg: String): String =
                arg.substringAfter('=', missingDelimiterValue = "").ifBlank {
                    error("Expected --name=value form, got: $arg")
                }

            args.forEach { arg ->
                when {
                    arg.startsWith("--samples=") -> samples = value(arg).toInt()
                    arg.startsWith("--players=") -> players = value(arg).toInt()
                    arg.startsWith("--seed=") -> seed = value(arg).toLong()
                    arg.startsWith("--strategy-seed=") -> strategySeed = value(arg).toLong()
                    arg.startsWith("--grove-seed=") -> groveSeed = value(arg).toLong()
                    arg.startsWith("--rounds=") -> rounds = value(arg)
                    arg.startsWith("--plant-overrides=") -> plantOverrides = Paths.get(value(arg))
                    arg.startsWith("--round-overrides=") -> roundOverrides = Paths.get(value(arg))
                    arg.startsWith("--weights=") -> weights = value(arg).split(',').map(Paths::get)
                    arg.startsWith("--labels=") -> labels = value(arg).split(',')
                    arg == "--help" -> {
                        println(
                            "evaluate_cultivation_main_crossplay " +
                                "--players=2|3|4 --samples=N " +
                                "--weights=PATH1,PATH2,... --labels=LABEL1,LABEL2,... " +
                                "[--plant-overrides=PATH] [--round-overrides=PATH] " +
                                "[--rounds=3/2/2] [--seed=N] [--strategy-seed=N] [--grove-seed=N]"
                        )
                        kotlin.system.exitProcess(0)
                    }
                    else -> error("Unknown argument: $arg")
                }
            }
            return CrossplayOptions(
                samples, players, seed, strategySeed, groveSeed, rounds,
                plantOverrides, roundOverrides, weights, labels
            )
        }
    }
}

private data class StrategyStats(
    var appearances: Int = 0,
    var winShare: Double = 0.0,
    var totalVp: Long = 0,
    var plantVp: Long = 0,
    var battleVp: Long = 0,
    var wispVp: Long = 0,
    var wounds: Long = 0,
    var sunlightGained: Long = 0,
    var sunlightSpent: Long = 0,
    var sunlightExtraMains: Long = 0,
    var finalPlantCount: Long = 0,
    var finalDicePower: Long = 0,
    var draw: Long = 0,
    var plant: Long = 0,
    val roundEffects: MutableMap<String, Long> = linkedMapOf()
)

private fun cultivationMainFactory(weights: LearnedCultivationMainWeights): PlayerDecisionFactory =
    object : PlayerDecisionFactory {
        override fun create() = create(StrategyRandomizer.create(), DecisionReasoningSink.NONE)

        override fun create(strategyRandomizer: StrategyRandomizer) =
            create(strategyRandomizer, DecisionReasoningSink.NONE)

        override fun create(
            strategyRandomizer: StrategyRandomizer,
            reasoningSink: DecisionReasoningSink
        ) = HumanBaselineDecisionDirector(
            strategyRandomizer = strategyRandomizer,
            reasoningSink = reasoningSink
        ).createDirector().let { baseline ->
            baseline.copy(cultivationMain = LearnedCultivationMainPolicy(weights))
        }
    }

private fun selectedEffect(entry: GameEntry.RoundEffectChoice): GameEffect? =
    when (entry.selectedMainAction) {
        MainActionKind.ROUND_EFFECT_1 -> entry.firstEffect
        MainActionKind.ROUND_EFFECT_2 -> entry.secondEffect
        else -> null
    }

fun main(args: Array<String>) {
    val o = CrossplayOptions.parse(args)
    val app = koinApplication { modules(appModules) }

    try {
        val koin = app.koin
        loadCultivationResearchCards(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())

        val plantManager = koin.get<PlantCardManager>()
        val plants = plantManager.getAllCards().cards
        val defaults = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
        val roundManager = koin.get<dugsolutions.leaf.v35.round.RoundCardManager>()
        val rounds = roundManager.getAllCards().cards

        val plantExp = PlantExperimentResearchConfig.resolve(o.plantOverrides, plants)
        val roundExp = RoundExperimentResearchConfig.resolve(o.roundOverrides, rounds)
        val effectiveCosts = plantExp.effectiveCosts(plants)

        val preparedWeights = o.weights.map { path ->
            LearnedCultivationMainCatalog.prepare(
                LearnedCultivationMainWeights.load(path),
                plants,
                effectiveCosts
            ).also {
                LearnedCultivationMainCatalog.validateCurrentSchema(it, plants, effectiveCosts)
            }
        }
        val factories = preparedWeights.map(::cultivationMainFactory)

        val gameFactory = koin.get<GameFactory>()
        val runner = koin.get<GameRunner>()
        val stats = linkedMapOf<String, StrategyStats>()
        o.labels.distinct().forEach { stats[it] = StrategyStats() }

        println("Leaf & Let Die — Cultivation Main Expert Crossplay")
        println("players=${o.players}; samples=${o.samples}; rounds=${o.rounds}")
        println("labels=${o.labels.joinToString(",")}")
        println("weights=${o.weights.joinToString(",")}")
        println("mechanical seeds=${o.seed}..${o.seed + o.samples - 1}")
        println("strategy seeds=${o.strategySeed}..${o.strategySeed + o.samples - 1}")
        println("grove seeds=${o.groveSeed}..${o.groveSeed + o.samples - 1}")
        println("All non-Cultivation-Main decisions = Human Baseline")
        println("Seat assignment rotates every sample.")
        println()

        val started = System.nanoTime()
        val progressEvery = max(1, o.samples / 20)

        repeat(o.samples) { sample ->
            // Rotate the lineup through physical seats so strategy identity is not confounded with seat.
            val assignment = List(o.players) { seat -> (seat + sample) % o.players }
            val sampleFactories = assignment.map { factories[it] }

            val grove = resolveResearchGroveForSample(
                GrovePlantCode.RANDOM_PATTERN,
                o.groveSeed,
                sample,
                plantManager,
                defaults,
                plants,
                plantExp.values
            )

            val game = gameFactory(
                GameConfig(
                    selectedPlantCards = grove,
                    playerDecisionFactories = sampleFactories,
                    roundSetup = o.roundSetup,
                    seed = o.seed + sample,
                    strategySeed = o.strategySeed + sample,
                    plantValues = plantExp.values,
                    roundValues = roundExp.values
                )
            )
            val result = runner.run(game)
            val summary = GameSummaryExtractor.extract(game, result)
            val entries = game.chronicle.entries

            summary.players.forEach { ps ->
                val strategyIndex = assignment[ps.seat]
                val label = o.labels[strategyIndex]
                val s = requireNotNull(stats[label])
                s.appearances++
                s.winShare += ps.winShare
                s.totalVp += ps.totalVp
                s.plantVp += ps.plantVp
                s.battleVp += ps.battleStrikeVp
                s.wispVp += ps.unplayedWispVp
                s.wounds += ps.woundsTaken
                s.sunlightGained += ps.sunlightGained
                s.sunlightSpent += ps.sunlightSpent
                s.sunlightExtraMains += ps.sunlightExtraMainActions
                s.finalPlantCount += ps.finalPlantCount
                s.finalDicePower += ps.finalDicePower

                entries.asSequence()
                    .filterIsInstance<GameEntry.RoundEffectChoice>()
                    .filter { it.playerId == ps.playerId && it.phase == ChroniclePhase.CULTIVATION }
                    .forEach { choice ->
                        when (choice.selectedMainAction) {
                            MainActionKind.DRAW -> s.draw++
                            MainActionKind.ACTIVATE_PLANT -> s.plant++
                            MainActionKind.ROUND_EFFECT_1,
                            MainActionKind.ROUND_EFFECT_2 -> {
                                selectedEffect(choice)?.let { effect ->
                                    val key = effect.name
                                    s.roundEffects[key] = (s.roundEffects[key] ?: 0L) + 1L
                                }
                            }
                            else -> Unit
                        }
                    }
            }

            val completed = sample + 1
            if (completed % progressEvery == 0 || completed == o.samples) {
                printCrossplayProgress(completed, o.samples, started)
            }
        }

        println()
        println("CROSSPLAY RESULTS")
        stats.forEach { (label, s) ->
            val n = s.appearances.coerceAtLeast(1)
            val mainTotal = s.draw + s.plant + s.roundEffects.values.sum()
            fun perAppearance(v: Long): Double = v.toDouble() / n
            fun pct(v: Long): Double = if (mainTotal == 0L) 0.0 else 100.0 * v / mainTotal.toDouble()

            println(
                "CROSSPLAY_RESULT " +
                    "label=$label " +
                    "appearances=${s.appearances} " +
                    "win_share_pct=${fmt(100.0 * s.winShare / n)} " +
                    "total_vp=${fmt(perAppearance(s.totalVp))} " +
                    "plant_vp=${fmt(perAppearance(s.plantVp))} " +
                    "battle_vp=${fmt(perAppearance(s.battleVp))} " +
                    "wisp_vp=${fmt(perAppearance(s.wispVp))} " +
                    "wounds=${fmt(perAppearance(s.wounds))} " +
                    "sunlight_gained=${fmt(perAppearance(s.sunlightGained))} " +
                    "sunlight_spent=${fmt(perAppearance(s.sunlightSpent))} " +
                    "sunlight_extra_mains=${fmt(perAppearance(s.sunlightExtraMains))} " +
                    "final_plant_count=${fmt(perAppearance(s.finalPlantCount))} " +
                    "final_dice_power=${fmt(perAppearance(s.finalDicePower))}"
            )
            println(
                "CROSSPLAY_ACTIONS " +
                    "label=$label " +
                    "draw_pct=${fmt(pct(s.draw))} " +
                    "plant_pct=${fmt(pct(s.plant))} " +
                    s.roundEffects.entries.sortedBy { it.key }
                        .joinToString(" ") { (effect, count) ->
                            "${effect}_pct=${fmt(pct(count))}"
                        }
            )
        }
    } finally {
        app.close()
    }
}

private fun fmt(value: Double): String = String.format("%.3f", value)

private fun printCrossplayProgress(completed: Int, total: Int, startedNanos: Long) {
    val elapsedSeconds = (System.nanoTime() - startedNanos).coerceAtLeast(0L) / 1_000_000_000.0
    val secondsPerGame = if (completed > 0) elapsedSeconds / completed else 0.0
    val remainingSeconds = secondsPerGame * (total - completed).coerceAtLeast(0)
    val eta = ZonedDateTime.now().plusSeconds(remainingSeconds.toLong())
    val percent = if (total > 0) completed * 100.0 / total else 100.0
    println(
        "CROSSPLAY_PROGRESS samples=$completed/$total (${String.format("%.1f", percent)}%) " +
            "elapsed=${formatDuration(elapsedSeconds)} " +
            "estRemaining=${formatDuration(remainingSeconds)} " +
            "ETA=${eta.format(DateTimeFormatter.ofPattern("EEE hh:mm a"))}"
    )
}

private fun formatDuration(seconds: Double): String {
    val whole = seconds.toLong().coerceAtLeast(0L)
    val d = Duration.ofSeconds(whole)
    val hours = d.toHours()
    val minutes = d.minusHours(hours).toMinutes()
    val secs = d.minusHours(hours).minusMinutes(minutes).seconds
    return when {
        hours > 0 -> String.format("%dh %02dm %02ds", hours, minutes, secs)
        minutes > 0 -> String.format("%dm %02ds", minutes, secs)
        else -> "${secs}s"
    }
}
