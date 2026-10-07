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
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.player.decision.learned.mulch.LearnedMulchTargetPolicy
import dugsolutions.leaf.v35.player.decision.learned.mulch.LearnedMulchTargetWeights
import dugsolutions.leaf.v35.player.decision.mulch.MulchTargetPolicy
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink
import dugsolutions.leaf.v35.round.RoundCardManager
import org.koin.dsl.koinApplication
import java.nio.file.Path
import java.nio.file.Paths

private data class TargetStats(
    var choices: Int = 0,
    var humanMatches: Int = 0,
    var face: Int = 0,
    var sides: Int = 0,
    var rerollGain: Double = 0.0,
) {
    fun add(chosen: EffectDieChoice, human: EffectDieChoice) {
        choices++
        if (chosen == human) humanMatches++
        face += chosen.value
        sides += chosen.sides
        rerollGain += (chosen.sides + 1) / 2.0 - chosen.value
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
        stats.add(learnedChoice, humanChoice)
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
        val stats = TargetStats()

        var learnedWins = 0.0
        var humanWins = 0.0
        var learnedVp = 0.0
        var humanVp = 0.0

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
                        stats,
                    ),
                )
            }
        }

        repeat(options.samples) { sample ->
            val seat = affectedSeat(sample, options.players)

            for (useLearnedTarget in listOf(false, true)) {
                val decisionFactories = List(options.players) { playerSeat ->
                    if (playerSeat == seat && useLearnedTarget) {
                        learnedFactory()
                    } else {
                        PlayerDecisionFactory.humanBaseline()
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

        if (stats.choices > 0) {
            val agreement = percent(stats.humanMatches.toDouble() / stats.choices)
            val avgFace = "%.3f".format(stats.face.toDouble() / stats.choices)
            val avgSides = "%.3f".format(stats.sides.toDouble() / stats.choices)
            val avgRerollGain = "%+.3f".format(stats.rerollGain / stats.choices)

            println("Learned Mulch target choices=${stats.choices}; Human-target agreement=$agreement")
            println("Selected avg face=$avgFace avg sides=$avgSides avg rerollGain=$avgRerollGain")
        }
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

                    else -> error("Unknown option: $argument")
                }
                index++
            }

            require(samples > 0)
            require(players in 2..4)

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
