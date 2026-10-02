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
 * The scenario deliberately uses the recommended first-game Plant set and
 * independent Human Baseline decision directors for the requested player count. It is a lifecycle/Chronicle
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
        recordDecisionReasoning: Boolean = false,
        chronicleDetail: Boolean = false,
        selectedPlantNames: List<String> = IntegrationCatalog.FIRST_GAME_PLANT_NAMES,
        numPlayers: Int = NUM_PLAYERS
    ): GameScenario =
        GameScenario(
            numPlayers = numPlayers,
            selectedPlantNames = selectedPlantNames,
            roundSetup = roundSetup,
            seed = seed,
            strategySeed = strategySeed,
            decisionFactories = List(numPlayers) {
                PlayerDecisionFactory.humanBaseline()
            },
            recordDecisionReasoning = recordDecisionReasoning,
            chronicleDetail = chronicleDetail
        )
}
