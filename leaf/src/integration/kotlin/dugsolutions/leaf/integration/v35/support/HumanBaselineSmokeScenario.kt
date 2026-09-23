package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.round.domain.RoundCardType

/**
 * Canonical deterministic full-game smoke scenario for the current Human Baseline.
 *
 * The exact Round card order enforces the requested 3/2/2 cadence:
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
        GameRoundSetup.Ordered(
            cultivationRounds = 7,
            battleRounds = 3
        )

    val exactRoundNames: List<String> = listOf(
        "Resource_Sunlight_Water",
        "Resource_Compost_Mulch",
        "Resource_Sunlight_Compost",
        "Battle_Bloom_Burrow",
        "Resource_Sunlight_Mulch",
        "Resource_Water_Compost",
        "Battle_Bloom_Surge",
        "Resource_Water_Mulch",
        "Resource_Compost_Mulch",
        "Battle_Whisper_Burst"
    )

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
        recordDecisionReasoning: Boolean = false
    ): GameScenario =
        GameScenario(
            numPlayers = NUM_PLAYERS,
            selectedPlantNames = IntegrationCatalog.FIRST_GAME_PLANT_NAMES,
            roundSetup = roundSetup,
            seed = seed,
            decisionFactories = List(NUM_PLAYERS) {
                PlayerDecisionFactory.humanBaseline()
            },
            exactRoundNames = exactRoundNames,
            recordDecisionReasoning = recordDecisionReasoning
        )
}
