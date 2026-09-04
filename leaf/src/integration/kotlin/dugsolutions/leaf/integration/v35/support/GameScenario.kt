package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.random.die.di.DieFactory

/**
 * Immutable top-level setup instructions for a deterministic integration game.
 *
 * A scenario deliberately names Plant definitions rather than embedding
 * mutable game state. [IntegrationGameHarness] resolves those names through
 * the real CSV-backed [IntegrationCatalog] and then calls the production
 * GameFactory.
 */
data class GameScenario(
    val numPlayers: Int = 2,
    val selectedPlantNames: List<String> =
        IntegrationCatalog.FIRST_GAME_PLANT_NAMES,
    val roundSetup: GameRoundSetup = GameRoundSetup.firstGame(),
    val seed: Long? = 1L,
    val dieConfig: DieFactory.Config = DieFactory.Config.RANDOM,
    val decisionFactories: List<PlayerDecisionFactory> = emptyList()
) {
    init {
        require(numPlayers in 2..4) {
            "Integration scenario requires 2 to 4 players: $numPlayers"
        }
        require(decisionFactories.isEmpty() || decisionFactories.size == numPlayers) {
            "Decision factory count must be empty or equal numPlayers: " +
                "factories=${decisionFactories.size}, players=$numPlayers"
        }
    }

    internal fun toGameConfig(catalog: IntegrationCatalog): GameConfig {
        val factories =
            if (decisionFactories.isEmpty()) {
                List(numPlayers) { PlayerDecisionFactory.baseline() }
            } else {
                decisionFactories
            }

        return GameConfig(
            selectedPlantCards = catalog.plants(selectedPlantNames),
            playerDecisionFactories = factories,
            roundSetup = roundSetup,
            seed = seed,
            dieConfig = dieConfig
        )
    }
}
