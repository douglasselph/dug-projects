package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.context.DecisionContextFactory
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** NC-21/22: Battle policies value current canonical Round effects, not old fixed dice. */
class BoundedRoundEffectPriorityTest {
    @Test
    fun surgeScoresOnlyActuallyAvailableBoundedDieAsFutureValue() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val observed = DecisionContextFactory.create(game, actor, phaseOverride = RoundCardType.BATTLE)
        val context = observed.copy(
            progress = observed.progress.copy(battleRoundsRemaining = 2),
            grove = observed.grove.copy(graftBed = mapOf(DieSides.D10 to 1, DieSides.D20 to 9))
        )
        val score = BattleRoundEffectPriority.score(context, GameEffect.GAIN_UP_TO_D12_TO_DISCARD)
        assertEquals(
            BattleResourceUtility.ORIGIN + BattleResourceUtility.futureDiscardDie(context, 10),
            score.total
        )
        val empty = context.copy(grove = context.grove.copy(graftBed = mapOf(DieSides.D20 to 9)))
        assertEquals(0, BattleRoundEffectPriority.score(empty, GameEffect.GAIN_UP_TO_D12_TO_DISCARD).total)
    }

    @Test
    fun swellScoresImmediateBattleOpportunityButNoValueWhenRowsAreLocked() {
        val actor = EffectTestFixture.player(1)
        val other = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, other)
        val battle = BattleState(listOf(actor, other))
        for (player in listOf(actor, other)) {
            repeat(3) { player.dice.addToHand(FixedEffectDie(6, 3)) }
        }
        battle.placeInitialHands()
        val observed = DecisionContextFactory.create(
            game, actor, battleState = battle, phaseOverride = RoundCardType.BATTLE
        )
        val context = observed.copy(grove = observed.grove.copy(graftBed = mapOf(DieSides.D6 to 1)))
        assertTrue(BattleRoundEffectPriority.score(context, GameEffect.GAIN_UP_TO_D8_AND_USE_NOW).total > 0)
        for (row in StrikeRow.entries) battle.grid.closeRow(row)
        val locked = DecisionContextFactory.create(
            game, actor, battleState = battle, phaseOverride = RoundCardType.BATTLE
        ).copy(grove = context.grove)
        assertEquals(0, BattleRoundEffectPriority.score(locked, GameEffect.GAIN_UP_TO_D8_AND_USE_NOW).total)
    }
}
