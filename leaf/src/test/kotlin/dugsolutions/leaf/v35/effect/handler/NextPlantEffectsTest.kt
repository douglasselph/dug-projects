package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.DefaultGameEffectExecutor
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.LastEffectChoiceStrategy
import dugsolutions.leaf.v35.tokens.Token
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** NC-13 and NC-15: test actual execution, scarcity, target legality and row locks. */
class NextPlantEffectsTest {
    private val executor = DefaultGameEffectExecutor()

    @Test
    fun somedaySunflowerGainsTwoPhysicalSunlightTokensAndRecordsLimitedSupply() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(game, actor, GameEffect.GAIN_TWO_SUNLIGHT_TOKENS)
        assertTrue(executor.supports(request.effect))
        game.grove.tokens.set(Token.SUNLIGHT, 3)
        assertTrue(executor.canExecute(request))
        executor.execute(request)
        assertEquals(2, actor.tokens.sunlightCount)
        assertEquals(1, game.grove.tokens.sunlightCount)

        // Remaining stock is one; there is no synthetic second token.
        executor.execute(request)
        assertEquals(3, actor.tokens.sunlightCount)
        assertEquals(0, game.grove.tokens.sunlightCount)
        assertFalse(executor.canExecute(request))
    }

    @Test
    fun somedaySunflowerTransfersOnlyOneWhenOnlyOneRemainsAndNoneWhenEmpty() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(game, actor, GameEffect.GAIN_TWO_SUNLIGHT_TOKENS)
        game.grove.tokens.set(Token.SUNLIGHT, 1)
        executor.execute(request)
        assertEquals(1, actor.tokens.sunlightCount)
        assertEquals(0, game.grove.tokens.sunlightCount)
        assertFalse(executor.canExecute(request))
    }

    @Test
    fun bloomBackflipInCultivationFlipsExactlyOneChosenDieWithoutRolling() {
        val first = FixedEffectDie(6, 2)
        val chosen = FixedEffectDie(8, 3)
        val actor = EffectTestFixture.player(1, listOf(first, chosen), LastEffectChoiceStrategy())
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val effect = GameEffect.FLIP_OWN_DIE_AND_OPPONENT_DIE_IN_BATTLE
        val request = EffectTestFixture.request(game, actor, effect)
        assertTrue(executor.supports(effect))
        assertTrue(executor.canExecute(request))
        executor.execute(request)
        assertEquals(2, first.value)
        assertEquals(6, chosen.value) // D8 opposite face: 9 - 3
        assertEquals(0, actor.tokens.waterCount)
    }

    @Test
    fun bloomBackflipInBattleFlipsOwnAndOpposingOpenDiceWithoutChangingClosedRows() {
        val own = FixedEffectDie(6, 2)
        val opposingOpen = FixedEffectDie(8, 7)
        val opposingLocked = FixedEffectDie(12, 11)
        val actor = EffectTestFixture.player(1, listOf(own))
        val opponent = EffectTestFixture.player(2, listOf(opposingOpen, opposingLocked))
        val game = EffectTestFixture.game(actor, opponent)
        val battle = BattleState(listOf(actor, opponent))
        battle.grid.placeDie(actor, StrikeRow.MIDDLE, own)
        battle.grid.placeDie(opponent, StrikeRow.MIDDLE, opposingOpen)
        battle.grid.placeDie(opponent, StrikeRow.TOP, opposingLocked)
        battle.grid.closeRow(StrikeRow.TOP)
        val request = EffectTestFixture.request(
            game, actor, GameEffect.FLIP_OWN_DIE_AND_OPPONENT_DIE_IN_BATTLE
        ).copy(phase = GameEffectPhase.BATTLE, battleState = battle)

        assertTrue(executor.canExecute(request))
        executor.execute(request)
        assertEquals(5, own.value) // 7 - 2
        assertEquals(2, opposingOpen.value) // 9 - 7
        assertEquals(11, opposingLocked.value)
    }

    @Test
    fun bloomBackflipRequiresLegalOpposingFlipInBattleAndDoesNotMutateOwnDieOnFailure() {
        val own = FixedEffectDie(6, 2)
        val opponentDie = FixedEffectDie(8, 7)
        val actor = EffectTestFixture.player(1, listOf(own))
        val opponent = EffectTestFixture.player(2, listOf(opponentDie))
        val game = EffectTestFixture.game(actor, opponent)
        val battle = BattleState(listOf(actor, opponent))
        battle.grid.placeDie(actor, StrikeRow.MIDDLE, own)
        battle.grid.placeDie(opponent, StrikeRow.TOP, opponentDie)
        battle.grid.closeRow(StrikeRow.TOP)
        val request = EffectTestFixture.request(
            game, actor, GameEffect.FLIP_OWN_DIE_AND_OPPONENT_DIE_IN_BATTLE
        ).copy(phase = GameEffectPhase.BATTLE, battleState = battle)
        assertFalse(executor.canExecute(request))
        assertEquals(2, own.value)
    }

    @Test
    fun bloomBackflipCannotFlipD4sWhoseOppositeFaceIsUndefined() {
        val actor = EffectTestFixture.player(1, listOf(FixedEffectDie(4, 2)))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        assertFalse(executor.canExecute(EffectTestFixture.request(
            game, actor, GameEffect.FLIP_OWN_DIE_AND_OPPONENT_DIE_IN_BATTLE
        )))
    }
}
