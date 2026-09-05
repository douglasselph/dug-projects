package dugsolutions.leaf.v35.player.decision.context

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DecisionContextFactoryTest {

    @Test
    fun create_copiesMutableGameStateIntoImmutableValues() {
        val handDie = FixedDie(20, 2)
        val supplyDie = FixedDie(6, 1)
        val actor = player(
            1,
            PlayerDice(
                supply = listOf(supplyDie),
                hand = listOf(handDie)
            )
        )
        val opponent = player(2, PlayerDice())
        actor.critters.add(Critter.BEE)
        val game = GameEngineTestFixture.game(players = listOf(actor, opponent))

        val context = DecisionContextFactory.create(
            game = game,
            actor = actor,
            phaseOverride = RoundCardType.CULTIVATION
        )

        handDie.adjustTo(19)
        actor.dice.addToHand(FixedDie(8, 8))
        actor.critters.add(Critter.BEE)
        actor.addVp(3)

        assertEquals(PlayerId(1), context.self.id)
        assertEquals(listOf(2), context.self.board.hand.map { it.value })
        assertEquals(listOf(20), context.self.board.hand.map { it.sides })
        assertEquals(1, context.self.board.bees)
        assertEquals(0, context.self.board.vp)
        assertEquals(1, context.opponents.size)
        assertEquals(PlayerId(2), context.opponents.single().id)
        assertEquals(RoundCardType.CULTIVATION, context.phase)
        assertFalse(context === DecisionContext.EMPTY)
    }

    @Test
    fun create_withBattleState_exposesRowsTotalsClosedAndWithdrawalWithoutLiveDice() {
        val p1Top = FixedDie(12, 8)
        val p1Middle = FixedDie(8, 4)
        val p2Top = FixedDie(10, 6)
        val p1 = player(1, PlayerDice(hand = listOf(p1Top, p1Middle)))
        val p2 = player(2, PlayerDice(hand = listOf(p2Top)))
        p1.critters.add(Critter.BEE)
        val game = GameEngineTestFixture.game(players = listOf(p1, p2))
        val battle = BattleState(listOf(p1, p2))
        battle.grid.placeDie(p1, StrikeRow.TOP, p1Top)
        battle.grid.placeDie(p1, StrikeRow.MIDDLE, p1Middle)
        battle.grid.placeDie(p2, StrikeRow.TOP, p2Top)
        battle.grid.placeCritter(p1, StrikeRow.TOP, Critter.BEE)
        battle.grid.withdrawPlayer(p1.id, StrikeRow.BOTTOM)
        battle.grid.closeRow(StrikeRow.MIDDLE)

        val context = DecisionContextFactory.create(
            game = game,
            actor = p1,
            battleState = battle,
            phaseOverride = RoundCardType.BATTLE
        )

        val top = requireNotNull(context.battle).row(StrikeRow.TOP)
        val p1TopView = requireNotNull(top.forPlayer(p1.id))
        val cbattle = requireNotNull(context.battle)
        val middle = cbattle.row(StrikeRow.MIDDLE)
        val bottom = cbattle.row(StrikeRow.BOTTOM)

        assertEquals(listOf(p1.id, p2.id), cbattle.playerOrder)
        assertEquals(listOf(8), p1TopView.dice.map { it.value })
        assertEquals(listOf(1), p1TopView.dice.map { it.handIndex })
        assertEquals(8, p1TopView.dieTotal)
        assertEquals(2, p1TopView.critterTotal)
        assertEquals(10, p1TopView.total)
        assertTrue(middle.closed)
        assertTrue(requireNotNull(bottom.forPlayer(p1.id)).withdrawn)

        // The snapshot stores scalar values, not live Die references.
        p1Top.adjustTo(1)
        assertEquals(8, p1TopView.dice.single().value)
    }

    private fun player(id: Int, dice: PlayerDice): Player =
        Player(
            id = PlayerId(id),
            decisions = DecisionDirector.mechanicalControl(),
            dice = dice
        )

    private class FixedDie(sides: Int, value: Int) : Die(sides) {
        init {
            adjustTo(value)
        }

        override fun roll(): Die = this
    }
}
