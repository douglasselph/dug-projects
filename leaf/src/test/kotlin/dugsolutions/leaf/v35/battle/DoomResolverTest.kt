package dugsolutions.leaf.v35.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DoomResolverTest {

    private val resolver = DoomResolver()

    @Test
    fun trashesEveryDieAtLowestValueAndStopsWhenAtLeastTwoWereTrashed() {
        val low1 = die(6, 1)
        val low2 = die(8, 1)
        val survivor = die(10, 5)
        val p1 = player(1, low1, survivor)
        val p2 = player(2, low2)
        val game = GameEngineTestFixture.game(players = listOf(p1, p2))
        val state = BattleState(listOf(p1, p2))
        state.grid.placeDie(p1, StrikeRow.TOP, low1)
        state.grid.placeDie(p1, StrikeRow.MIDDLE, survivor)
        state.grid.placeDie(p2, StrikeRow.TOP, low2)

        val result = resolver.execute(game, state)

        assertEquals(2, result.count)
        assertEquals(listOf(1), result.valuesTrashed)
        assertFalse(p1.dice.hand.any { it === low1 })
        assertFalse(p2.dice.hand.any { it === low2 })
        assertTrue(p1.dice.hand.any { it === survivor })
        assertEquals(1, state.grid.diePlacements.size)
        assertTrue(state.grid.locationOf(survivor) != null)
    }

    @Test
    fun ifLowestGroupHasOnlyOneContinuesWithEntireNextLowestGroup() {
        val one = die(6, 1)
        val threeA = die(8, 3)
        val threeB = die(10, 3)
        val high = die(12, 9)
        val p1 = player(1, one, threeA)
        val p2 = player(2, threeB, high)
        val game = GameEngineTestFixture.game(players = listOf(p1, p2))
        val state = BattleState(listOf(p1, p2))
        state.grid.placeDie(p1, StrikeRow.TOP, one)
        state.grid.placeDie(p1, StrikeRow.MIDDLE, threeA)
        state.grid.placeDie(p2, StrikeRow.TOP, threeB)
        state.grid.placeDie(p2, StrikeRow.MIDDLE, high)

        val result = resolver.execute(game, state)

        assertEquals(3, result.count)
        assertEquals(listOf(1, 3), result.valuesTrashed)
        assertEquals(listOf(high), p2.dice.hand)
        assertEquals(1, state.grid.diePlacements.size)
    }

    @Test
    fun doomUsesUniversalTrashRuleSoD4ReturnsToGraftBed() {
        val d4 = die(4, 1)
        val d6 = die(6, 2)
        val p1 = player(1, d4)
        val p2 = player(2, d6)
        val game = GameEngineTestFixture.game(players = listOf(p1, p2))
        val state = BattleState(listOf(p1, p2))
        state.grid.placeDie(p1, StrikeRow.TOP, d4)
        state.grid.placeDie(p2, StrikeRow.TOP, d6)
        assertEquals(0, game.grove.graftBed.count(DieSides.D4))

        val result = resolver.execute(game, state)

        assertEquals(2, result.count)
        assertEquals(1, game.grove.graftBed.count(DieSides.D4))
        assertTrue(result.dice.first { it.sides == DieSides.D4 }.returnedToGraftBed)
        assertFalse(result.dice.first { it.sides == DieSides.D6 }.returnedToGraftBed)
    }

    @Test
    fun ifOnlyOneDieExistsDoomTrashesThatOneAndStops() {
        val only = die(6, 4)
        val p1 = player(1, only)
        val p2 = player(2)
        val game = GameEngineTestFixture.game(players = listOf(p1, p2))
        val state = BattleState(listOf(p1, p2))
        state.grid.placeDie(p1, StrikeRow.BOTTOM, only)

        val result = resolver.execute(game, state)

        assertEquals(1, result.count)
        assertTrue(state.grid.diePlacements.isEmpty())
        assertTrue(p1.dice.hand.isEmpty())
    }

    @Test
    fun noGridDiceIsNoOp() {
        val p1 = player(1)
        val p2 = player(2)
        val game = GameEngineTestFixture.game(players = listOf(p1, p2))
        val state = BattleState(listOf(p1, p2))

        assertTrue(resolver.execute(game, state).dice.isEmpty())
    }

    private fun player(id: Int, vararg dice: Die): Player =
        Player(
            id = PlayerId(id),
            decisions = DecisionDirector.baseline(),
            dice = PlayerDice(hand = dice.toList())
        )

    private fun die(sides: Int, value: Int): Die =
        object : Die(sides) {
            init { adjustTo(value) }
            override fun roll(): Die = this
        }
}
