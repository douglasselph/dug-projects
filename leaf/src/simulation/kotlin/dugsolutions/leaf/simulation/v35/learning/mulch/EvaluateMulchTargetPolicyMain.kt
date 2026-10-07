package dugsolutions.leaf.simulation.v35.learning.mulch

import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.resolveResearchGroveForSample
import dugsolutions.leaf.simulation.v35.experiment.round.RoundExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.learning.buy.parseRoundSetup
import dugsolutions.leaf.simulation.v35.learning.cultivation.affectedSeat
import dugsolutions.leaf.simulation.v35.learning.cultivation.loadCultivationResearchCards
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselineDecisionDirector
import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.player.decision.learned.mulch.LearnedMulchTargetPolicy
import dugsolutions.leaf.v35.player.decision.learned.mulch.LearnedMulchTargetWeights
import dugsolutions.leaf.v35.player.decision.mulch.MulchTargetPolicy
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.koin.dsl.koinApplication
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.TreeMap

private data class DieCell(val sides: Int, val face: Int) : Comparable<DieCell> {
    override fun compareTo(other: DieCell): Int =
        compareValuesBy(this, other, DieCell::sides, DieCell::face)
}

private class TargetStats(private val label: String) {
    var choices: Int = 0
    var humanMatches: Int = 0
    var faceTotal: Long = 0
    var sidesTotal: Long = 0
    var rerollGainTotal: Double = 0.0
    var faceFractionTotal: Double = 0.0
    var handCountTotal: Long = 0
    var handFaceTotalBefore: Long = 0
    var handFaceTotalAfter: Long = 0
    var legalCountTotal: Long = 0
    var storedMulchCountTotal: Long = 0
    var storedMulchSidesTotal: Long = 0
    var battleNextCount: Int = 0
    var lowestFaceCount: Int = 0
    var highestFaceCount: Int = 0
    var lowestSidesCount: Int = 0
    var highestSidesCount: Int = 0
    var purchaseTierLossChoices: Int = 0
    var purchaseTierStepsLostTotal: Long = 0
    var bestTierLossTotal: Long = 0

    private val selectedCells = TreeMap<DieCell, Int>()
    private val availableCells = TreeMap<DieCell, Int>()
    private val selectedBySides = TreeMap<Int, Int>()
    private val faceBySides = TreeMap<Int, Long>()

    fun add(
        request: ChooseEffectDieRequest,
        chosen: EffectDieChoice,
        humanReference: EffectDieChoice? = null,
    ) {
        choices++
        if (humanReference != null && chosen == humanReference) humanMatches++

        val legal = request.legalChoices
        val context = request.context
        val board = context.self.board
        val beforePower = PurchaseThresholdHeuristics.purchasingPower(board)
        val afterPower = (beforePower - chosen.value).coerceAtLeast(0)
        val tiers = PurchaseThresholdHeuristics.availableCostTiers(context.grove)
        val change = PurchaseThresholdHeuristics.change(beforePower, afterPower, tiers)
        val beforeTier = change.beforeTier ?: 0
        val afterTier = change.afterTier ?: 0
        val storedSides = (board.mulch + board.pendingMulch).mapNotNull { it.storedDieSides?.value }

        faceTotal += chosen.value
        sidesTotal += chosen.sides
        rerollGainTotal += (chosen.sides + 1) / 2.0 - chosen.value
        faceFractionTotal += chosen.value.toDouble() / chosen.sides
        handCountTotal += board.hand.size
        handFaceTotalBefore += board.hand.sumOf { it.value }
        handFaceTotalAfter += (board.hand.sumOf { it.value } - chosen.value).coerceAtLeast(0)
        legalCountTotal += legal.size
        storedMulchCountTotal += storedSides.size
        storedMulchSidesTotal += storedSides.sum()

        if (context.progress.upcomingRoundTypes.firstOrNull() == RoundCardType.BATTLE) battleNextCount++
        if (chosen.value == legal.minOf { it.value }) lowestFaceCount++
        if (chosen.value == legal.maxOf { it.value }) highestFaceCount++
        if (chosen.sides == legal.minOf { it.sides }) lowestSidesCount++
        if (chosen.sides == legal.maxOf { it.sides }) highestSidesCount++

        val tierStepsLost = change.crossedDown.size
        val bestTierLoss = (beforeTier - afterTier).coerceAtLeast(0)
        if (tierStepsLost > 0 || bestTierLoss > 0) purchaseTierLossChoices++
        purchaseTierStepsLostTotal += tierStepsLost
        bestTierLossTotal += bestTierLoss

        val selectedCell = DieCell(chosen.sides, chosen.value)
        selectedCells[selectedCell] = (selectedCells[selectedCell] ?: 0) + 1
        selectedBySides[chosen.sides] = (selectedBySides[chosen.sides] ?: 0) + 1
        faceBySides[chosen.sides] = (faceBySides[chosen.sides] ?: 0L) + chosen.value
        legal.forEach { candidate ->
            val cell = DieCell(candidate.sides, candidate.value)
            availableCells[cell] = (availableCells[cell] ?: 0) + 1
        }
    }

    fun print(includeHumanAgreement: Boolean) {
        if (choices == 0) {
            println("TARGET_SUMMARY policy=$label choices=0")
            return
        }
        fun pct(count: Int): String = "%.2f%%".format(count * 100.0 / choices)
        fun avg(total: Long): String = "%.3f".format(total.toDouble() / choices)
        fun avg(total: Double): String = "%+.3f".format(total / choices)

        println("TARGET_SUMMARY policy=$label choices=$choices")
        if (includeHumanAgreement) {
            println("TARGET_HUMAN_AGREEMENT policy=$label rate=${pct(humanMatches)} matches=$humanMatches choices=$choices")
        }
        println(
            "TARGET_AVERAGES policy=$label " +
                "face=${avg(faceTotal)} sides=${avg(sidesTotal)} " +
                "faceFraction=${"%.3f".format(faceFractionTotal / choices)} " +
                "rerollGain=${avg(rerollGainTotal)} handCount=${avg(handCountTotal)} " +
                "handFaceBefore=${avg(handFaceTotalBefore)} handFaceAfter=${avg(handFaceTotalAfter)} " +
                "legalCandidates=${avg(legalCountTotal)} storedMulchCount=${avg(storedMulchCountTotal)} " +
                "storedMulchSides=${avg(storedMulchSidesTotal)}"
        )
        println(
            "TARGET_CONTEXT policy=$label " +
                "battleNext=${pct(battleNextCount)} lowestFace=${pct(lowestFaceCount)} " +
                "highestFace=${pct(highestFaceCount)} lowestSides=${pct(lowestSidesCount)} " +
                "highestSides=${pct(highestSidesCount)} purchaseTierLoss=${pct(purchaseTierLossChoices)} " +
                "avgTierStepsLost=${"%.3f".format(purchaseTierStepsLostTotal.toDouble() / choices)} " +
                "avgBestTierLoss=${"%.3f".format(bestTierLossTotal.toDouble() / choices)}"
        )

        selectedBySides.forEach { (sides, count) ->
            val avgFace = (faceBySides[sides] ?: 0L).toDouble() / count
            println(
                "TARGET_DIE_SIZE policy=$label sides=$sides selected=$count " +
                    "share=${pct(count)} avgFace=${"%.3f".format(avgFace)}"
            )
        }

        (selectedCells.keys + availableCells.keys).toSortedSet().forEach { cell ->
            val selected = selectedCells[cell] ?: 0
            val available = availableCells[cell] ?: 0
            if (selected > 0 || available > 0) {
                val rate = if (available == 0) 0.0 else selected * 100.0 / available
                println(
                    "TARGET_CELL policy=$label sides=${cell.sides} face=${cell.face} " +
                        "selected=$selected available=$available selectWhenAvailable=${"%.2f".format(rate)}%"
                )
            }
        }
    }
}

private class RecordingHuman(
    private val stats: TargetStats,
) : MulchTargetPolicy {
    override fun chooseDie(
        request: ChooseEffectDieRequest,
        fallback: EffectStrategy,
    ): EffectDieChoice {
        val choice = fallback.chooseDie(request)
        stats.add(request, choice)
        return choice
    }
}

private class RecordingLearned(
    private val learned: MulchTargetPolicy,
    private val stats: TargetStats,
) : MulchTargetPolicy {
    override fun chooseDie(
        request: ChooseEffectDieRequest,
        fallback: EffectStrategy,
    ): EffectDieChoice {
        val humanChoice = fallback.chooseDie(request)
        val fixedFallback = object : EffectStrategy by fallback {
            override fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice = humanChoice
        }
        val learnedChoice = learned.chooseDie(request, fixedFallback)
        stats.add(request, learnedChoice, humanChoice)
        return learnedChoice
    }
}

fun main(args: Array<String>) {
    val options = EvalOptions.parse(args.toList())
    val app = koinApplication { modules(appModules) }

    try {
        val koin = app.koin
        loadCultivationResearchCards(
            koin.get(),
            koin.get(),
            koin.get(),
            koin.get(),
            koin.get(),
            koin.get(),
        )

        val plantManager = koin.get<PlantCardManager>()
        val roundManager = koin.get<RoundCardManager>()
        val plants = plantManager.getAllCards().cards
        val rounds = roundManager.getAllCards().cards

        val plantExperiment = PlantExperimentResearchConfig.resolve(options.plantOverrides, plants)
        val roundExperiment = RoundExperimentResearchConfig.resolve(options.roundOverrides, rounds)
        val defaultPlants = FirstGameDefault.PLANT_NAMES.map { name ->
            requireNotNull(plantManager.getCard(name))
        }
        val groves = List(options.samples) { sample ->
            resolveResearchGroveForSample(
                options.grovePattern,
                options.groveSeed,
                sample,
                plantManager,
                defaultPlants,
                plants,
                plantExperiment.values,
            )
        }

        val weights = LearnedMulchTargetWeights.load(options.weights)
        val gameFactory = koin.get<GameFactory>()
        val runner = koin.get<GameRunner>()
        val humanStats = TargetStats("Human")
        val learnedStats = TargetStats("Learned")

        var learnedWins = 0.0
        var humanWins = 0.0
        var learnedVp = 0.0
        var humanVp = 0.0

        fun humanFactory(): PlayerDecisionFactory = object : PlayerDecisionFactory {
            override fun create() = create(StrategyRandomizer.create(), DecisionReasoningSink.NONE)
            override fun create(strategyRandomizer: StrategyRandomizer) =
                create(strategyRandomizer, DecisionReasoningSink.NONE)
            override fun create(
                strategyRandomizer: StrategyRandomizer,
                reasoningSink: DecisionReasoningSink,
            ) = HumanBaselineDecisionDirector(
                strategyRandomizer = strategyRandomizer,
                reasoningSink = reasoningSink,
            ).createDirector().let { baseline ->
                baseline.copy(mulchTarget = RecordingHuman(humanStats))
            }
        }

        fun learnedFactory(): PlayerDecisionFactory = object : PlayerDecisionFactory {
            override fun create() = create(StrategyRandomizer.create(), DecisionReasoningSink.NONE)
            override fun create(strategyRandomizer: StrategyRandomizer) =
                create(strategyRandomizer, DecisionReasoningSink.NONE)
            override fun create(
                strategyRandomizer: StrategyRandomizer,
                reasoningSink: DecisionReasoningSink,
            ) = HumanBaselineDecisionDirector(
                strategyRandomizer = strategyRandomizer,
                reasoningSink = reasoningSink,
            ).createDirector().let { baseline ->
                baseline.copy(
                    mulchTarget = RecordingLearned(
                        LearnedMulchTargetPolicy(weights),
                        learnedStats,
                    ),
                )
            }
        }

        val evaluationStartedNanos = System.nanoTime()
        val progressStep = maxOf(1, options.samples / 20)
        val etaFormatter = DateTimeFormatter.ofPattern("EEE HH:mm:ss")

        fun formatDuration(totalSeconds: Long): String {
            val seconds = totalSeconds.coerceAtLeast(0)
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val remainder = seconds % 60
            return "%02d:%02d:%02d".format(hours, minutes, remainder)
        }

        repeat(options.samples) { sample ->
            val seat = affectedSeat(sample, options.players)

            for (useLearnedTarget in listOf(false, true)) {
                val decisionFactories = List(options.players) { playerSeat ->
                    when {
                        playerSeat != seat -> PlayerDecisionFactory.humanBaseline()
                        useLearnedTarget -> learnedFactory()
                        else -> humanFactory()
                    }
                }

                val game = gameFactory(
                    GameConfig(
                        selectedPlantCards = groves[sample],
                        playerDecisionFactories = decisionFactories,
                        roundSetup = options.roundSetup,
                        seed = options.seed + sample,
                        strategySeed = options.strategySeed + sample,
                        plantValues = plantExperiment.values,
                        roundValues = roundExperiment.values,
                    )
                )
                val result = runner.run(game)
                val playerSummary = GameSummaryExtractor.extract(game, result).players.single {
                    it.seat == seat
                }

                if (useLearnedTarget) {
                    learnedWins += playerSummary.winShare
                    learnedVp += playerSummary.totalVp
                } else {
                    humanWins += playerSummary.winShare
                    humanVp += playerSummary.totalVp
                }
            }

            val completed = sample + 1
            if (completed % progressStep == 0 || completed == options.samples) {
                val elapsedSeconds = ((System.nanoTime() - evaluationStartedNanos) / 1_000_000_000L).coerceAtLeast(1L)
                val remainingSamples = options.samples - completed
                val estimatedRemainingSeconds =
                    if (completed == 0) 0L else (elapsedSeconds * remainingSamples / completed)
                val percentComplete = completed * 100.0 / options.samples
                val eta = LocalDateTime.now().plusSeconds(estimatedRemainingSeconds).format(etaFormatter)
                println(
                    "EVAL_PROGRESS samples=$completed/${options.samples} " +
                        "(${"%.1f".format(percentComplete)}%) " +
                        "elapsed=${formatDuration(elapsedSeconds)} " +
                        "estRemaining=${formatDuration(estimatedRemainingSeconds)} ETA=$eta"
                )
                System.out.flush()
            }
        }

        fun percent(value: Double): String = "%.2f%%".format(value * 100.0)
        val humanWin = percent(humanWins / options.samples)
        val learnedWin = percent(learnedWins / options.samples)
        val humanVpText = "%.3f".format(humanVp / options.samples)
        val learnedVpText = "%.3f".format(learnedVp / options.samples)
        val winDeltaText = "%+.2f pp".format((learnedWins - humanWins) * 100.0 / options.samples)

        println("Leaf & Let Die — Mulch Target held-out evaluation")
        println("players=${options.players} samples=${options.samples} rounds=${options.rounds}")
        println("Human target: win=$humanWin VP=$humanVpText")
        println("Learned target: win=$learnedWin VP=$learnedVpText delta=$winDeltaText")
        println()
        humanStats.print(includeHumanAgreement = false)
        println()
        learnedStats.print(includeHumanAgreement = true)
    } finally {
        app.close()
    }
}

private data class EvalOptions(
    val samples: Int,
    val seed: Long,
    val strategySeed: Long,
    val weights: Path,
    val plantOverrides: Path?,
    val roundOverrides: Path?,
    val grovePattern: String?,
    val groveSeed: Long,
    val players: Int,
    val rounds: String,
) {
    val roundSetup = parseRoundSetup(rounds)

    companion object {
        fun parse(args: List<String>): EvalOptions {
            var samples = 1000
            var seed = 219000L
            var strategySeed = 229000L
            var weights = Paths.get("output/ai/mulch-target-policy-v1-trained.weights")
            var plantOverrides: Path? = Paths.get("data/research/4p/resync/resync-current.csv")
            var roundOverrides: Path? = Paths.get("data/research/4p/resync/round-resync-current.csv")
            var grovePattern: String? = GrovePlantCode.RANDOM_PATTERN
            var groveSeed = 239000L
            var players = 4
            var rounds = "3/2/2"
            var index = 0

            fun valueFor(argument: String): String =
                if ('=' in argument) argument.substringAfter('=') else args[++index]

            while (index < args.size) {
                val argument = args[index]
                when {
                    argument.startsWith("--samples") || argument.startsWith("--games") ->
                        samples = valueFor(argument).toInt()
                    argument.startsWith("--seed") ->
                        seed = valueFor(argument).toLong()
                    argument.startsWith("--strategy-seed") ->
                        strategySeed = valueFor(argument).toLong()
                    argument.startsWith("--weights") ->
                        weights = Paths.get(valueFor(argument))
                    argument.startsWith("--plant-overrides") ->
                        plantOverrides = Paths.get(valueFor(argument))
                    argument.startsWith("--round-overrides") ->
                        roundOverrides = Paths.get(valueFor(argument))
                    argument == "--first-game-grove" ->
                        grovePattern = null
                    argument == "--random-grove" ->
                        grovePattern = GrovePlantCode.RANDOM_PATTERN
                    argument.startsWith("--grove-pattern") ->
                        grovePattern = valueFor(argument)
                    argument.startsWith("--grove-seed") ->
                        groveSeed = valueFor(argument).toLong()
                    argument.startsWith("--players") ->
                        players = valueFor(argument).toInt()
                    argument.startsWith("--rounds") ->
                        rounds = valueFor(argument)
                    else -> error("Unknown argument: $argument")
                }
                index++
            }

            require(samples > 0)
            require(players in 2..4)
            require(weights.toFile().isFile) { "Missing Mulch Target weights: $weights" }
            return EvalOptions(
                samples = samples,
                seed = seed,
                strategySeed = strategySeed,
                weights = weights,
                plantOverrides = plantOverrides,
                roundOverrides = roundOverrides,
                grovePattern = grovePattern,
                groveSeed = groveSeed,
                players = players,
                rounds = rounds,
            )
        }
    }
}
