package dugsolutions.leaf.simulation.v35.experiment

import dugsolutions.leaf.simulation.v35.analysis.GameSummary
import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.domain.PlantCard

/**
 * Production high-volume batch boundary.
 *
 * Each Game and its Chronicle live only for one loop iteration. After the
 * compact [GameSummary] is extracted, neither object is retained by this
 * runner or its result.
 */
class GameSummaryBatchRunner(
    private val gameFactory: GameFactory,
    private val gameRunner: GameRunner,
    selectedPlantCards: List<PlantCard>,
    private val roundSetup: GameRoundSetup = GameRoundSetup.standard()
) : BatchRunner<BatchRunResult> {
    private val selectedPlantCards = selectedPlantCards.toList()

    override fun run(matchup: Matchup, config: ExperimentConfig): BatchRunResult {
        val summaries = ArrayList<GameSummary>(config.games)

        repeat(config.games) { sample ->
            val gameConfig = GameConfig(
                selectedPlantCards = selectedPlantCards,
                playerDecisionFactories = matchup.players.map { it.decisionFactory },
                roundSetup = roundSetup,
                seed = config.mechanicalSeedAt(sample),
                strategySeed = config.strategySeedAt(sample),
                recordDecisionReasoning = false
            )
            val game = gameFactory(gameConfig)
            val runResult = gameRunner.run(game)
            summaries += GameSummaryExtractor.extract(game, runResult)
        }

        return BatchRunResult(
            matchupName = matchup.name,
            summaries = summaries
        )
    }
}
