package dugsolutions.leaf.v35.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FirstEffectChoiceStrategy
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.handler.CrossPlayerEffectHandler
import dugsolutions.leaf.v35.effect.handler.DieValueEffectHandler
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** v38 cross-effect tests: exercise real effect handlers followed by row-only Doom/cleanup. */
class V38CrossEffectStrikeFinishTest {

    @Test
    fun `Pollen Theft changes die controller before per Strike Doom and cleanup`() {
        val ownTop = FixedEffectDie(8, 4)
        val opponentTop = FixedEffectDie(6, 3)
        val opponentMiddle = FixedEffectDie(8, 1)
        val actor = EffectTestFixture.player(1, listOf(ownTop), FirstEffectChoiceStrategy())
        val opponent = EffectTestFixture.player(2, listOf(opponentTop, opponentMiddle))
        val game = EffectTestFixture.game(actor, opponent)
        val battle = BattleState(listOf(actor, opponent))
        battle.grid.placeDie(actor, StrikeRow.TOP, ownTop)
        battle.grid.placeDie(opponent, StrikeRow.TOP, opponentTop)
        battle.grid.placeDie(opponent, StrikeRow.MIDDLE, opponentMiddle)
        battle.activateThreeEngagements()

        val request = EffectTestFixture.request(
            game, actor, GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE
        ).copy(phase = GameEffectPhase.BATTLE, battleState = battle)
        val handler = CrossPlayerEffectHandler()
        assertTrue(handler.canExecute(request))
        handler.execute(request, GameEffectExecutor { })
        assertEquals(1, opponentMiddle.value, "Pollen Theft never rerolls")
        assertEquals(actor.id, battle.grid.locationOf(opponentMiddle)?.playerId)
        assertEquals(StrikeRow.TOP, battle.grid.locationOf(opponentMiddle)?.row)
        assertEquals(opponent.id, battle.grid.locationOf(ownTop)?.playerId)
        assertEquals(StrikeRow.MIDDLE, battle.grid.locationOf(ownTop)?.row)

        val finished = StrikeFinishCoordinator().finish(game, battle, StrikeRow.TOP) {
            DecisionContext.EMPTY
        }
        assertEquals(listOf(actor.id), finished.doom.dice.map { it.playerId })
        assertEquals(listOf(1), finished.doom.dice.map { it.value })
        assertEquals(1, finished.cleanup.totalDiscardedDice)
        assertFalse(actor.dice.hand.any { it === opponentMiddle })
        assertFalse(actor.dice.discard.any { it === opponentMiddle }, "Doom trashes, not discards")
        assertTrue(opponent.dice.discard.any { it === opponentTop })
        assertTrue(opponent.dice.hand.any { it === ownTop }, "The future row remains active")
        assertTrue(battle.grid.isRowClosed(StrikeRow.TOP))
        assertFalse(battle.grid.isRowClosed(StrikeRow.MIDDLE))
        assertEquals(4, ownTop.value)
        assertEquals(1, battle.frozenResolvedRow(StrikeRow.TOP)!!.squares.first().dice.single().value)
    }

    @Test
    fun `Root and Scoot withdrawal prevents Wound but opponent die still faces single-die Doom`() {
        val ownTop = FixedEffectDie(8, 4)
        val ownMiddle = FixedEffectDie(10, 7)
        val opponentTop = FixedEffectDie(12, 9)
        val opponentMiddle = FixedEffectDie(8, 6)
        val actor = EffectTestFixture.player(1, listOf(ownTop, ownMiddle), FirstEffectChoiceStrategy())
        val opponent = EffectTestFixture.player(2, listOf(opponentTop, opponentMiddle))
        val game = EffectTestFixture.game(actor, opponent)
        val battle = BattleState(listOf(actor, opponent))
        battle.grid.placeDie(actor, StrikeRow.TOP, ownTop)
        battle.grid.placeDie(actor, StrikeRow.MIDDLE, ownMiddle)
        battle.grid.placeDie(opponent, StrikeRow.TOP, opponentTop)
        battle.grid.placeDie(opponent, StrikeRow.MIDDLE, opponentMiddle)
        battle.activateThreeEngagements()

        val request = EffectTestFixture.request(
            game, actor, GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE
        ).copy(phase = GameEffectPhase.BATTLE, battleState = battle)
        val handler = DieValueEffectHandler()
        assertTrue(handler.canExecute(request))
        handler.execute(request, GameEffectExecutor { })
        assertEquals(5, ownTop.value, "Raise resolves before withdrawal")
        assertTrue(battle.grid.isPlayerWithdrawn(actor.id, StrikeRow.TOP))
        assertTrue(actor.dice.discard.any { it === ownTop })

        val finished = StrikeFinishCoordinator().finish(game, battle, StrikeRow.TOP) {
            DecisionContext.EMPTY
        }
        assertEquals(listOf(opponent.id), finished.resolution.winnerIds)
        assertTrue(finished.resolution.wounds.isEmpty(), "Withdrawn player cannot be Wounded")
        assertEquals(2, opponent.vp)
        assertEquals(0, actor.vp)
        assertEquals(listOf(9), finished.doom.dice.map { it.value },
            "Only the one remaining die must be Doomed under v38")
        assertEquals(0, finished.cleanup.totalDiscardedDice)
        assertTrue(battle.grid.square(actor.id, StrikeRow.TOP).isEmpty)
        assertTrue(battle.grid.square(opponent.id, StrikeRow.TOP).isEmpty)
        assertTrue(actor.dice.hand.any { it === ownMiddle })
        assertTrue(opponent.dice.hand.any { it === opponentMiddle })
        assertFalse(battle.grid.isRowClosed(StrikeRow.MIDDLE))
    }
}
