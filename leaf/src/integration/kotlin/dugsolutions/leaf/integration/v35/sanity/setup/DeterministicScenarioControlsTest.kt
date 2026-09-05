package dugsolutions.leaf.integration.v35.sanity.setup

import dugsolutions.leaf.integration.v35.support.GameScenario
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.game.GameRoundSetup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class DeterministicScenarioControlsTest {

    @Test
    fun `exact decks and scripted randomizer are installed without setup randomness`() {
        val randomizer = ScriptedRandomizer()
        val scenario = GameScenario(
            numPlayers = 2,
            roundSetup = GameRoundSetup.Ordered(
                cultivationRounds = 1,
                battleRounds = 1
            ),
            exactRoundNames = listOf(
                "Resource_Compost_Mulch",
                "Battle_Bloom_Burrow"
            ),
            exactWispNames = listOf(
                "Wisp_Award_VP",
                "Wisp_Award_VP2"
            ),
            randomizerFactory = { randomizer }
        )

        IntegrationGameHarness(scenario).use { harness ->
            assertSame(randomizer, harness.randomizer)
            assertSame(randomizer, harness.game.randomizer)
            assertEquals(
                scenario.exactRoundNames,
                harness.game.roundDeck.cards.cards.map { it.name }
            )
            assertEquals(
                scenario.exactWispNames,
                harness.game.grove.wispDeck.cards.cards.map { it.name }
            )

            // Construction required no random answers because both decks are exact.
            randomizer.assertExhausted()

            // The next physical die roll is now independently controllable.
            randomizer.rolls(3)
            val die = harness.game.players.first().dice.supply.first()
            die.roll()
            assertEquals(3, die.value)
            randomizer.assertExhausted()
        }
    }
}
