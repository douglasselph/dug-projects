package dugsolutions.leaf.v35.game.round.battle

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.RoundExecutor
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BattleRoundTest {

    @Test
    fun executeRound_runsOpeningDrawRankPlaceActionsStrikesDoomThenCleanup() {
        val p1 = player(
            id = 1,
            supply = listOf(
                die(10, 9),
                die(12, 8),
                die(20, 7)
            )
        )
        val p2 = player(
            id = 2,
            supply = listOf(
                die(10, 6),
                die(12, 5),
                die(20, 4)
            )
        )
        val game = game(p1, p2)
        val card = checkNotNull(game.roundDeck.next())
        val round = BattleRound(NoOpEffectExecutor())

        val result = round.executeRound(game, card)

        assertEquals(mapOf(p1.id to 3, p2.id to 3), result.openingDrawCounts)
        assertEquals(listOf(p1.id, p2.id), result.battleOrder)
        assertEquals(6, result.initialPlacements.size)
        assertEquals(2, result.actions.firstMainActions.size)
        assertEquals(2, result.actions.finalMainActions.size)
        assertEquals(3, result.strikes.strikes.size)
        assertEquals(2, result.doom.count)
        assertEquals(4, result.cleanup.totalDiscardedDice)

        // P1 wins all three Strikes: 2 VP each, no Wounds at these margins.
        assertEquals(6, p1.vp)
        assertEquals(0, p2.vp)

        // Doom removed P2's 4 and 5; every surviving Grid die was reclaimed.
        assertEquals(3, p1.dice.discardSize)
        assertEquals(1, p2.dice.discardSize)
        assertTrue(p1.dice.hand.isEmpty())
        assertTrue(p2.dice.hand.isEmpty())

        val messages = markerMessages(game)
        val drawIndex = messages.indexOfFirst { it.startsWith("BATTLE_OPENING_DRAW_COMPLETE") }
        val placeIndex = messages.indexOfFirst { it.startsWith("BATTLE_RANK_PLACE") }
        val actionIndex = messages.indexOfFirst { it.startsWith("BATTLE_MAIN_ACTION") }
        val strikeIndex = messages.indexOfFirst { it.startsWith("STRIKE row=") }
        val doomIndex = messages.indexOfFirst { it.startsWith("DOOM ") }
        val cleanupIndex = messages.indexOfFirst { it.startsWith("BATTLE_CLEANUP") }

        assertTrue(drawIndex >= 0)
        assertTrue(drawIndex < placeIndex)
        assertTrue(placeIndex < actionIndex)
        assertTrue(actionIndex < strikeIndex)
        assertTrue(strikeIndex < doomIndex)
        assertTrue(doomIndex < cleanupIndex)
    }

    @Test
    fun execute_asRoundExecutor_plugsDirectlyIntoRoundCoordinator() {
        val p1 = player(1, listOf(die(10, 9), die(12, 8), die(20, 7)))
        val p2 = player(2, listOf(die(10, 6), die(12, 5), die(20, 4)))
        val game = game(p1, p2)
        var cultivationCalled = false
        val coordinator = RoundCoordinator(
            cultivation = RoundExecutor { _, _ -> cultivationCalled = true },
            battle = BattleRound(NoOpEffectExecutor())
        )

        val execution = checkNotNull(coordinator.executeNext(game))

        assertEquals(RoundCardType.BATTLE, execution.card.type)
        assertEquals(1, execution.roundNumber)
        assertTrue(!cultivationCalled)
        val messages = markerMessages(game)
        assertTrue(messages.first().startsWith("ROUND_REVEALED"))
        assertTrue(messages.any { it.startsWith("BATTLE_RANK_PLACE") })
        assertTrue(messages.any { it.startsWith("STRIKE row=") })
        assertTrue(messages.any { it.startsWith("DOOM ") })
        assertTrue(messages.any { it.startsWith("BATTLE_CLEANUP") })
        assertTrue(messages.last().startsWith("ROUND_COMPLETED"))
    }

    @Test
    fun executeRound_rejectsCultivationCardBeforePlayerMutation() {
        val p1 = player(1, listOf(die(10, 9), die(12, 8), die(20, 7)))
        val p2 = player(2, listOf(die(10, 6), die(12, 5), die(20, 4)))
        val game = GameEngineTestFixture.game(
            cultivationRounds = 1,
            battleRounds = 0,
            players = listOf(p1, p2)
        )
        val card = checkNotNull(game.roundDeck.next())
        assertEquals(RoundCardType.CULTIVATION, card.type)

        assertFailsWith<IllegalArgumentException> {
            BattleRound(NoOpEffectExecutor()).executeRound(game, card)
        }

        assertEquals(3, p1.dice.supplySize)
        assertEquals(3, p2.dice.supplySize)
        assertTrue(p1.dice.hand.isEmpty())
        assertTrue(p2.dice.hand.isEmpty())
        assertTrue(markerMessages(game).isEmpty())
    }

    private fun game(first: Player, second: Player): Game =
        GameEngineTestFixture.game(
            cultivationRounds = 0,
            battleRounds = 1,
            players = listOf(first, second)
        )

    private fun player(
        id: Int,
        supply: List<Die>
    ): Player =
        Player(
            id = PlayerId(id),
            decisions = DecisionDirector.baseline(),
            dice = PlayerDice(supply = supply)
        )

    private fun die(sides: Int, value: Int): Die =
        object : Die(sides) {
            init { adjustTo(value) }
            override fun roll(): Die = this
        }

    private fun markerMessages(game: Game): List<String> =
        game.chronicle.entries
            .filterIsInstance<GameEntry.Marker>()
            .map { it.message }

    private class NoOpEffectExecutor : GameEffectExecutor {
        override fun execute(request: GameEffectRequest) = Unit
    }
}
