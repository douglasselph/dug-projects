package dugsolutions.leaf.integration.v35.sanity.cultivation

import dugsolutions.leaf.integration.v35.support.GameScenario
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.game.GameRoundSetup

internal fun cultivationHarness(
    randomizer: ScriptedRandomizer = ScriptedRandomizer(),
    roundName: String = "Resource_Water_Mulch",
    wispNames: List<String> = emptyList(),
    first: ScriptedDecisionDirector = ScriptedDecisionDirector(),
    second: ScriptedDecisionDirector = ScriptedDecisionDirector()
): IntegrationGameHarness =
    IntegrationGameHarness(
        GameScenario(
            numPlayers = 2,
            roundSetup = GameRoundSetup.Ordered(
                cultivationRounds = 1,
                battleRounds = 0
            ),
            exactRoundNames = listOf(roundName),
            exactWispNames = wispNames,
            randomizerFactory = { randomizer },
            decisionFactories = listOf(
                first.singleGameFactory(),
                second.singleGameFactory()
            )
        )
    )

internal fun ScriptedDecisionDirector.finishBuildWithDraws() {
    cultivation.thenMain(dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction.Draw)
    cultivation.thenMain(dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction.Draw)
    cultivation.thenDone()
}

internal fun ScriptedDecisionDirector.finishBuildWithWater() {
    cultivation.thenMain(dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction.RoundEffect1)
    cultivation.thenMain(dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction.RoundEffect1)
    cultivation.thenDone()
}
