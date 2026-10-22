package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.*
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import org.junit.jupiter.api.Test
import kotlin.test.*

/** Regression contract for both independently selectable Sapping Snapdragon effects. */
class SappingSnapdragonVariantsRegressionTest {
    private val same = GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW
    private val other = GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW
    private val executor = DefaultGameEffectExecutor()

    @Test
    fun cultivation_bothVariantsOnlyRaiseSelectedDie() {
        for (effect in listOf(same, other)) {
            val die = FixedEffectDie(12, 3)
            val actor = EffectTestFixture.player(1, listOf(die))
            val opponentDie = FixedEffectDie(10, 8)
            val opponent = EffectTestFixture.player(2, listOf(opponentDie))
            val request = EffectTestFixture.request(EffectTestFixture.game(actor, opponent), actor, effect)
            assertTrue(executor.supports(effect))
            assertTrue(executor.canExecute(request))
            executor.execute(request)
            assertEquals(5, die.value)
            assertEquals(8, opponentDie.value)
        }
    }

    @Test
    fun battle_sameDieTargetsItsOwnRowAndUsesActualReduction() {
        val selected = FixedEffectDie(20, 3)
        val anchor = FixedEffectDie(12, 4)
        val actor = EffectTestFixture.player(1, listOf(selected, anchor), HighestSidesDieChoice())
        val topOpp = FixedEffectDie(10, 5)
        val middleOpp = FixedEffectDie(8, 7)
        val opponent = EffectTestFixture.player(2, listOf(topOpp, middleOpp))
        val game = EffectTestFixture.game(actor, opponent)
        val battle = BattleState(listOf(actor, opponent))
        battle.grid.placeDie(actor, StrikeRow.TOP, selected)
        battle.grid.placeDie(actor, StrikeRow.MIDDLE, anchor)
        battle.grid.placeDie(opponent, StrikeRow.TOP, topOpp)
        battle.grid.placeDie(opponent, StrikeRow.MIDDLE, middleOpp)
        executor.execute(EffectTestFixture.request(game, actor, same).copy(phase = GameEffectPhase.BATTLE, battleState = battle))
        assertEquals(7, selected.value) // +2, then +2 actually reduced in TOP
        assertEquals(4, anchor.value)
        assertEquals(3, topOpp.value)
        assertEquals(7, middleOpp.value)
    }

    @Test
    fun battle_otherDieTargetsDifferentRowAndCreditsReductionToAnchor() {
        val raised = FixedEffectDie(20, 3)
        val anchor = FixedEffectDie(12, 4)
        val actor = EffectTestFixture.player(1, listOf(raised, anchor), HighestSidesDieChoice())
        val topOpp = FixedEffectDie(10, 5)
        val middleOpp = FixedEffectDie(8, 7)
        val opponent = EffectTestFixture.player(2, listOf(topOpp, middleOpp))
        val game = EffectTestFixture.game(actor, opponent)
        val battle = BattleState(listOf(actor, opponent))
        battle.grid.placeDie(actor, StrikeRow.TOP, raised)
        battle.grid.placeDie(actor, StrikeRow.MIDDLE, anchor)
        battle.grid.placeDie(opponent, StrikeRow.TOP, topOpp)
        battle.grid.placeDie(opponent, StrikeRow.MIDDLE, middleOpp)
        assertTrue(executor.canExecute(EffectTestFixture.request(game, actor, other).copy(phase = GameEffectPhase.BATTLE, battleState = battle)))
        executor.execute(EffectTestFixture.request(game, actor, other).copy(phase = GameEffectPhase.BATTLE, battleState = battle))
        assertEquals(5, raised.value) // initial +2 only
        assertEquals(6, anchor.value) // +2 actually reduced in MIDDLE
        assertEquals(5, topOpp.value)
        assertEquals(5, middleOpp.value)
    }

    @Test
    fun battle_otherDieRequiresTwoPlacedActorDice_butSameDieRequiresOne() {
        val die = FixedEffectDie(12, 4)
        val actor = EffectTestFixture.player(1, listOf(die))
        val opponent = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, opponent)
        val battle = BattleState(listOf(actor, opponent))
        battle.grid.placeDie(actor, StrikeRow.TOP, die)
        assertTrue(executor.canExecute(EffectTestFixture.request(game, actor, same).copy(phase = GameEffectPhase.BATTLE, battleState = battle)))
        assertFalse(executor.canExecute(EffectTestFixture.request(game, actor, other).copy(phase = GameEffectPhase.BATTLE, battleState = battle)))
    }

    @Test
    fun battle_emptyOpponentRowStillGrantsInitialRaiseOnly() {
        for (effect in listOf(same, other)) {
            val first = FixedEffectDie(20, 3)
            val second = FixedEffectDie(12, 4)
            val actor = EffectTestFixture.player(1, listOf(first, second), HighestSidesDieChoice())
            val opponent = EffectTestFixture.player(2)
            val game = EffectTestFixture.game(actor, opponent)
            val battle = BattleState(listOf(actor, opponent))
            battle.grid.placeDie(actor, StrikeRow.TOP, first)
            battle.grid.placeDie(actor, StrikeRow.MIDDLE, second)
            executor.execute(EffectTestFixture.request(game, actor, effect).copy(phase = GameEffectPhase.BATTLE, battleState = battle))
            assertEquals(5, first.value)
            assertEquals(4, second.value)
        }
    }

    @Test
    fun battle_fullStrikeSquaresRemainUnchangedInCapacityAndLocation() {
        for (effect in listOf(same, other)) {
            val first = FixedEffectDie(20, 3)
            val second = FixedEffectDie(12, 4)
            val actorFill = listOf(FixedEffectDie(6, 2), FixedEffectDie(6, 2))
            val opponentFill = listOf(FixedEffectDie(6, 5), FixedEffectDie(6, 5), FixedEffectDie(6, 5))
            val actor = EffectTestFixture.player(1, listOf(first, second) + actorFill, HighestSidesDieChoice())
            val opponent = EffectTestFixture.player(2, opponentFill)
            val game = EffectTestFixture.game(actor, opponent)
            val battle = BattleState(listOf(actor, opponent))
            (listOf(first) + actorFill).forEach { battle.grid.placeDie(actor, StrikeRow.TOP, it) }
            battle.grid.placeDie(actor, StrikeRow.MIDDLE, second)
            opponentFill.forEach { battle.grid.placeDie(opponent, StrikeRow.MIDDLE, it) }
            executor.execute(EffectTestFixture.request(game, actor, effect).copy(phase = GameEffectPhase.BATTLE, battleState = battle))
            assertEquals(3, battle.grid.square(actor.id, StrikeRow.TOP).dice.size)
            assertEquals(3, battle.grid.square(opponent.id, StrikeRow.MIDDLE).dice.size)
            assertEquals(StrikeRow.TOP, battle.grid.locationOf(first)?.row)
            assertEquals(StrikeRow.MIDDLE, battle.grid.locationOf(second)?.row)
        }
    }

    /** Select by die size rather than Hand index: PlayerDice sorts its initial Hand. */
    private class HighestSidesDieChoice : FirstEffectChoiceStrategy() {
        override fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice =
            request.legalChoices.maxBy { it.sides }
    }
}
