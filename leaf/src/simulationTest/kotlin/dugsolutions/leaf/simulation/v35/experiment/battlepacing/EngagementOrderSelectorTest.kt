package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EngagementOrderSelectorTest {
    private class FixedDie(sides: Int, value: Int) : Die(sides) {
        init { adjustTo(value) }
        override fun roll(): Die = this
    }

    private fun player(id: Int, vararg values: Int): Player = Player(
        id = PlayerId(id),
        decisions = DecisionDirector.mechanicalControl(),
        dice = PlayerDice(hand = values.map { FixedDie(20, it) })
    )

    @Test
    fun `row local maximum wins even when another player has stronger other row`() {
        val a = player(1, 11, 10, 1)
        val b = player(2, 9, 8, 7)
        val c = player(3, 8, 7, 6)
        val state = BattleState(listOf(a, b, c))
        state.placeInitialHands()
        // Physical table is 2 -> 3 -> 1; grid is 1 -> 2 -> 3.
        val physicalSeats = listOf(b.id, c.id, a.id)
        val result = EngagementOrderSelector.select(state, physicalSeats, StrikeRow.BOTTOM)
        assertEquals(b.id, result.starter)
        assertEquals(listOf(b.id, c.id, a.id), result.clockwiseActors)
        assertEquals(mapOf(a.id to 1, b.id to 7, c.id to 6), result.highestShowingByPlayer)
    }

    @Test
    fun `tie uses leftmost grid column and ignores other dice`() {
        val a = player(1, 15, 8, 1)
        val b = player(2, 14, 8, 7)
        val c = player(3, 13, 8, 6)
        val state = BattleState(listOf(c, a, b))
        state.placeInitialHands()
        val order = EngagementOrderSelector.select(state, listOf(a.id, b.id, c.id), StrikeRow.MIDDLE)
        assertEquals(c.id, order.starter)
        assertEquals(listOf(c.id, a.id, b.id), order.clockwiseActors)
    }

    @Test
    fun `zero dice count zero and ties resolve by leftmost column for two players`() {
        val a = player(1)
        val b = player(2)
        val state = BattleState(listOf(b, a))
        state.placeInitialHands()
        val order = EngagementOrderSelector.select(state, listOf(a.id, b.id), StrikeRow.BOTTOM)
        assertEquals(b.id, order.starter)
        assertEquals(listOf(b.id, a.id), order.clockwiseActors)
        assertEquals(0, order.highestShowingByPlayer.getValue(a.id))
    }

    @Test
    fun `four player row order is frozen but future engagement recalculates`() {
        val p1 = player(1, 10, 5, 4)
        val p2 = player(2, 9, 6, 3)
        val p3 = player(3, 8, 7, 2)
        val p4 = player(4, 7, 4, 1)
        val state = BattleState(listOf(p1, p2, p3, p4))
        state.placeInitialHands()
        val seats = listOf(p4.id, p1.id, p3.id, p2.id)
        val first = EngagementOrderSelector.select(state, seats, StrikeRow.TOP)
        assertEquals(p1.id, first.starter)
        // Legal face change, not a fabricated off-grid die.
        state.grid.square(p4.id, StrikeRow.TOP).dice.single().adjustTo(20)
        assertEquals(p1.id, first.starter)
        assertEquals(listOf(p1.id, p3.id, p2.id, p4.id), first.clockwiseActors)
        assertEquals(p4.id, EngagementOrderSelector.select(state, seats, StrikeRow.TOP).starter)
        state.grid.square(p2.id, StrikeRow.MIDDLE).dice.single().adjustTo(16)
        assertEquals(p2.id, EngagementOrderSelector.select(state, seats, StrikeRow.MIDDLE).starter)
        assertEquals(10, first.highestShowingByPlayer.getValue(p1.id))
        assertTrue(first.traceLines().first().contains("starter=1"))
    }

    @Test
    fun `invalid physical seats or closed row rejected`() {
        val p1 = player(1, 5, 4, 3)
        val p2 = player(2, 6, 5, 4)
        val state = BattleState(listOf(p1, p2))
        state.placeInitialHands()
        assertFailsWith<IllegalArgumentException> {
            EngagementOrderSelector.select(state, listOf(p1.id, p1.id), StrikeRow.TOP)
        }
        assertFailsWith<IllegalArgumentException> {
            EngagementOrderSelector.select(state, listOf(p1.id, PlayerId(99)), StrikeRow.TOP)
        }
        state.grid.closeRow(StrikeRow.TOP)
        assertFailsWith<IllegalArgumentException> {
            EngagementOrderSelector.select(state, listOf(p1.id, p2.id), StrikeRow.TOP)
        }
    }
}
