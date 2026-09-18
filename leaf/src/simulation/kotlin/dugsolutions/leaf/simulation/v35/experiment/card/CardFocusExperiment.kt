package dugsolutions.leaf.simulation.v35.experiment.card

import dugsolutions.leaf.simulation.v35.strategy.StrategyProfile
import dugsolutions.leaf.simulation.v35.strategy.planned.CreaturePlan
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.domain.PlantCard

/**
 * Runs a focused Planned-Baseline player against Human Baseline opponents.
 *
 * The focused strategy is rotated through every seat. For a given sample
 * number every seat rotation receives the same mechanical and strategy base
 * seed, reducing seat/order noise in comparative experiments.
 */
class CardFocusExperiment(
    private val gameFactory: GameFactory,
    private val gameRunner: GameRunner
) : CardExperiment {
    override fun run(
        spec: CardFocusExperimentSpec,
        selectedPlantCards: List<PlantCard>
    ): CardExperimentResult {
        require(selectedPlantCards.any { it.name == spec.targetCard.cardName }) {
            "Focused card is not present in selected Plant cards: ${spec.targetCard.cardName}"
        }

        val plannedProfile = StrategyProfile.plannedBaseline(
            plan = CreaturePlan.of(spec.targetCard.cardName to spec.targetCount),
            name = "Focus ${spec.targetCard.cardName}×${spec.targetCount}"
        )
        val baselineProfile = StrategyProfile.humanBaseline()
        val observations = ArrayList<CardFocusGameObservation>(spec.totalGames)

        repeat(spec.gamesPerSeat) { sample ->
            val mechanicalSeed = seedAt(spec.baseSeed, sample)
            val strategySeed = seedAt(spec.strategyBaseSeed, sample)

            repeat(spec.numPlayers) { focusSeat ->
                val factories = List(spec.numPlayers) { seat ->
                    if (seat == focusSeat) plannedProfile.decisionFactory
                    else baselineProfile.decisionFactory
                }
                val config = GameConfig(
                    selectedPlantCards = selectedPlantCards,
                    playerDecisionFactories = factories,
                    roundSetup = spec.roundSetup,
                    seed = mechanicalSeed,
                    strategySeed = strategySeed,
                    recordDecisionReasoning = false
                )
                val game = gameFactory(config)
                val result = gameRunner.run(game)
                observations += CardGameMetrics.observe(
                    game = game,
                    runResult = result,
                    focusSeat = focusSeat,
                    targetCard = spec.targetCard
                )
            }
        }

        return CardExperimentAggregator.aggregate(
            targetCard = spec.targetCard,
            targetCount = spec.targetCount,
            numPlayers = spec.numPlayers,
            observations = observations
        )
    }

    private fun seedAt(base: Long, sample: Int): Long =
        base + sample.toLong()
}
