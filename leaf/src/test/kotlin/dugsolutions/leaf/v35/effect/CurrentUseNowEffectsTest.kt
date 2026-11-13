package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** NC-16/17: current cards, not historical research-only versions. */
class CurrentUseNowEffectsTest {
    private val executor = DefaultGameEffectExecutor()

    @Test
    fun actualCatalogIdsRouteToCurrentMechanicsPoliciesAndDecisionContracts() {
        val registry = PlantCardRegistry().apply {
            loadFromCsv(CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST))
        }
        val scorer = HumanBaselineCardScorerRegistry()
        val expected = mapOf(
            "Flower_14_04" to GameEffect.GAIN_D4_USE_NOW_THEN_CYCLE_D4S,
            "Flower_17_02" to GameEffect.USE_DIE_FROM_DISCARD_NOW
        )
        for ((id, effect) in expected) {
            val card = assertNotNull(registry.getCard(id))
            assertEquals(effect, card.effect, id)
            assertEquals(effect, scorer.forPlant(card).effect, id)
            assertTrue(executor.supports(effect), id)
            assertFalse(effect in CardEffectReadiness.pending, id)
            assertNotNull(GameEffectDecisionRequirements.forEffect(effect), id)
        }
        assertEquals(setOf(EffectDecisionMechanism.EFFECT_DICE_SET),
            GameEffectDecisionRequirements.forEffect(GameEffect.GAIN_D4_USE_NOW_THEN_CYCLE_D4S)?.cultivation)
        assertEquals(setOf(EffectDecisionMechanism.EFFECT_DIE),
            GameEffectDecisionRequirements.forEffect(GameEffect.USE_DIE_FROM_DISCARD_NOW)?.cultivation)
    }

    @Test
    fun petalCannotStartWithoutRealGroveD4AndDoesNotMutateState() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(game, actor, GameEffect.GAIN_D4_USE_NOW_THEN_CYCLE_D4S)
        assertEquals(0, game.grove.graftBed.count(DieSides.D4))
        assertFalse(executor.canExecute(request))
        assertEquals(0, actor.dice.handSize)
    }

    @Test
    fun petalNewlyGainedD4MayBeDiscardedAndItsReplacementIsDrawn() {
        val actor = EffectTestFixture.player(1)
        // The ordinary Draw must pull from Supply before the just-discarded D4.
        val replacement = FixedEffectDie(8, 6)
        actor.dice.addToSupply(replacement)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        game.grove.graftBed.returnD4()
        val request = EffectTestFixture.request(game, actor, GameEffect.GAIN_D4_USE_NOW_THEN_CYCLE_D4S)
        assertTrue(executor.canExecute(request))
        executor.execute(request)
        assertEquals(0, game.grove.graftBed.count(DieSides.D4))
        assertEquals(listOf(replacement), actor.dice.hand)
        assertEquals(1, actor.dice.discard.count { it.sides == 4 })
        assertEquals(2, game.chronicle.entries.filterIsInstance<dugsolutions.leaf.v35.chronicle.domain.GameEntry.DieRolled>().size)
    }

    @Test
    fun petalBattleCannotGainWhenAllOpenSquaresAreFull() {
        val actor = EffectTestFixture.player(1)
        val other = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, other)
        val battle = BattleState(listOf(actor, other))
        game.grove.graftBed.returnD4()
        StrikeRow.entries.forEach { row ->
            repeat(3) {
                val die = FixedEffectDie(6, 5)
                actor.dice.addToHand(die)
                battle.grid.placeDie(actor, row, die)
            }
        }
        val request = EffectTestFixture.request(game, actor,
            GameEffect.GAIN_D4_USE_NOW_THEN_CYCLE_D4S).copy(
                phase = GameEffectPhase.BATTLE, battleState = battle)
        assertFalse(executor.canExecute(request))
        assertEquals(1, game.grove.graftBed.count(DieSides.D4))
        assertEquals(9, actor.dice.handSize)
    }

    @Test
    fun petalBattlePlacesReplacementOnlyInAnUnresolvedStrike() {
        val actor = EffectTestFixture.player(1)
        val other = EffectTestFixture.player(2)
        actor.dice.addToSupply(FixedEffectDie(8, 5))
        val game = EffectTestFixture.game(actor, other)
        val battle = BattleState(listOf(actor, other))
        val lockedD4 = FixedEffectDie(4, 2)
        actor.dice.addToHand(lockedD4)
        battle.grid.placeDie(actor, StrikeRow.TOP, lockedD4)
        battle.grid.closeRow(StrikeRow.TOP)
        game.grove.graftBed.returnD4()
        val request = EffectTestFixture.request(game, actor,
            GameEffect.GAIN_D4_USE_NOW_THEN_CYCLE_D4S).copy(
                phase = GameEffectPhase.BATTLE, battleState = battle)
        executor.execute(request)
        assertEquals(1, battle.grid.square(actor.id, StrikeRow.TOP).dieCount)
        assertTrue(actor.dice.hand.any { it === lockedD4 })
        assertEquals(2, lockedD4.value) // A resolved Strike cannot be cycled.
        assertEquals(1, battle.grid.square(actor.id, StrikeRow.MIDDLE).dieCount)
        assertTrue(actor.dice.hand.any { it.sides == 8 })
    }

    @Test
    fun forgetMeNotUsesTheExactDiscardDieNowWithoutAcquiringAnotherCopy() {
        val actor = EffectTestFixture.player(1)
        val returned = FixedEffectDie(12, 5)
        actor.dice.addToDiscard(returned)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(game, actor, GameEffect.USE_DIE_FROM_DISCARD_NOW)
        assertTrue(executor.canExecute(request))
        executor.execute(request)
        assertTrue(actor.dice.discard.isEmpty())
        assertEquals(1, actor.dice.handSize)
        assertTrue(actor.dice.hand.single() === returned)
        assertEquals(5, returned.value)
    }

    @Test
    fun forgetMeNotRecoveredDieRollGrantsItsNormalCritterReward() {
        val actor = EffectTestFixture.player(1)
        actor.dice.addToDiscard(FixedEffectDie(6, 1))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        executor.execute(EffectTestFixture.request(game, actor, GameEffect.USE_DIE_FROM_DISCARD_NOW))
        assertEquals(1, actor.dice.handSize)
        assertEquals(1, actor.critters.all.size)
    }

    @Test
    fun forgetMeNotBattleRequiresCapacityAndNeverReopensFinishedStrikes() {
        val actor = EffectTestFixture.player(1)
        val other = EffectTestFixture.player(2)
        actor.dice.addToDiscard(FixedEffectDie(12, 5))
        val game = EffectTestFixture.game(actor, other)
        val battle = BattleState(listOf(actor, other))
        battle.grid.closeRow(StrikeRow.TOP)
        val request = EffectTestFixture.request(game, actor,
            GameEffect.USE_DIE_FROM_DISCARD_NOW).copy(
                phase = GameEffectPhase.BATTLE, battleState = battle)
        assertTrue(executor.canExecute(request))
        executor.execute(request)
        assertEquals(0, battle.grid.square(actor.id, StrikeRow.TOP).dieCount)
        assertEquals(1, battle.grid.square(actor.id, StrikeRow.MIDDLE).dieCount)
        assertEquals(0, actor.dice.discardSize)
    }
}
