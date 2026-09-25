package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.round.domain.RoundCardType

/**
 * Canonical seeded full-game smoke scenario for the current Human Baseline.
 *
 * Production Round setup selects shuffled physical Round cards while enforcing
 * the requested 3/2/2 phase cadence:
 *
 * 3 Cultivation -> Battle -> 2 Cultivation -> Battle -> 2 Cultivation -> Battle.
 *
 * The scenario deliberately uses the recommended first-game Plant set and four
 * independent Human Baseline decision directors. It is a lifecycle/Chronicle
 * smoke scenario, not a balance experiment.
 */
object HumanBaselineSmokeScenario {

    const val DEFAULT_SEED: Long = 13_579L
    const val NUM_PLAYERS: Int = 4

    val roundSetup: GameRoundSetup =
        GameRoundSetup.patterned(3, 2, 2)

    val expectedRoundTypes: List<RoundCardType> = listOf(
        RoundCardType.CULTIVATION,
        RoundCardType.CULTIVATION,
        RoundCardType.CULTIVATION,
        RoundCardType.BATTLE,
        RoundCardType.CULTIVATION,
        RoundCardType.CULTIVATION,
        RoundCardType.BATTLE,
        RoundCardType.CULTIVATION,
        RoundCardType.CULTIVATION,
        RoundCardType.BATTLE
    )

    fun scenario(
        seed: Long = DEFAULT_SEED,
        strategySeed: Long = seed,
        recordDecisionReasoning: Boolean = false
    ): GameScenario =
        GameScenario(
            numPlayers = NUM_PLAYERS,
            selectedPlantNames = IntegrationCatalog.FIRST_GAME_PLANT_NAMES,
            roundSetup = roundSetup,
            seed = seed,
            strategySeed = strategySeed,
            decisionFactories = List(NUM_PLAYERS) {
                PlayerDecisionFactory.humanBaseline()
            },
            recordDecisionReasoning = recordDecisionReasoning
        )
}
