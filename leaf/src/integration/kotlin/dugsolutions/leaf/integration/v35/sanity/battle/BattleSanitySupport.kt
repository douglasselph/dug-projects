package dugsolutions.leaf.integration.v35.sanity.battle

import dugsolutions.leaf.integration.v35.support.GameScenario
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.game.GameRoundSetup

internal fun battleHarness(
    numPlayers: Int = 2,
    randomizer: ScriptedRandomizer = ScriptedRandomizer(),
    roundName: String = "Battle_Bloom_Burrow",
    wispNames: List<String> = emptyList(),
    decisions: List<ScriptedDecisionDirector> =
        List(numPlayers) { ScriptedDecisionDirector() }
): IntegrationGameHarness =
    IntegrationGameHarness(
        GameScenario(
            numPlayers = numPlayers,
            roundSetup = GameRoundSetup.Ordered(
                cultivationRounds = 0,
                battleRounds = 1
            ),
            exactRoundNames = listOf(roundName),
            exactWispNames = wispNames,
            randomizerFactory = { randomizer },
            decisionFactories = decisions.map { it.singleGameFactory() }
        )
    )
