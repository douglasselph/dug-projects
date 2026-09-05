package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.random.Randomizer
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
    val decisionFactories: List<PlayerDecisionFactory> = emptyList(),
    /** Optional exact Round draw order, expressed as stable CSV names. */
    val exactRoundNames: List<String>? = null,
    /** Optional exact Wisp draw order, expressed as stable CSV names. */
    val exactWispNames: List<String>? = null,
    /**
     * Optional per-harness Randomizer factory. When absent, GameFactory uses
     * the normal seeded production Randomizer.
     */
    val randomizerFactory: (() -> Randomizer)? = null
) {
    init {
        require(numPlayers in 2..4) {
            "Integration scenario requires 2 to 4 players: $numPlayers"
        }
        require(decisionFactories.isEmpty() || decisionFactories.size == numPlayers) {
            "Decision factory count must be empty or equal numPlayers: " +
                "factories=${decisionFactories.size}, players=$numPlayers"
        }
        exactRoundNames?.let { names ->
            require(names.size == roundSetup.totalRounds) {
                "Exact Round deck size must match roundSetup.totalRounds: " +
                    "exact=${names.size}, configured=${roundSetup.totalRounds}"
            }
        }
    }

    internal fun toGameConfig(catalog: IntegrationCatalog): GameConfig {
        val factories =
            if (decisionFactories.isEmpty()) {
                List(numPlayers) { PlayerDecisionFactory.mechanicalControl() }
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
