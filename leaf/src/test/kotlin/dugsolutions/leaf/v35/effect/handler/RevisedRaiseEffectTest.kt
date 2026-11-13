package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.DefaultGameEffectExecutor
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.LastEffectChoiceStrategy
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** NC-07/09/12: real effect dispatch, graft counts, caps, target choice and locked rows. */
class RevisedRaiseEffectTest {
    private val engine = DefaultGameEffectExecutor()

    @Test
    fun vineYieldRaisesOneChosenDieByNumberOfGraftedFlowersIncludingSpentFlowers() {
        val first = FixedEffectDie(6, 2)
        val chosen = FixedEffectDie(8, 5)
        val actor = EffectTestFixture.player(1, listOf(first, chosen), LastEffectChoiceStrategy())
        graft(actor, PlantType.VINE, "Vine")
        val flower1 = graft(actor, PlantType.FLOWER, "Flower 1")
        graft(actor, PlantType.FLOWER, "Flower 2")
        actor.creature.faceUp(flower1.id)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val effect = GameEffect.RAISE_ONE_DIE_PLUS_1_PER_GRAFTED_FLOWER
        val request = EffectTestFixture.request(game, actor, effect)

        assertTrue(engine.supports(effect))
        assertTrue(engine.canExecute(request))
        engine.execute(request)
        assertEquals(2, first.value)
        assertEquals(7, chosen.value)
        // Cap is applied once to the total, and a different die is not raised.
        engine.execute(request)
        assertEquals(8, chosen.value)
        assertEquals(2, first.value)
    }

    @Test
    fun vineYieldIsNotExecutableWithoutGraftedFlowers() {
        val actor = EffectTestFixture.player(1, listOf(FixedEffectDie(8, 2)))
        graft(actor, PlantType.VINE, "Vine")
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        assertFalse(engine.canExecute(EffectTestFixture.request(
            game, actor, GameEffect.RAISE_ONE_DIE_PLUS_1_PER_GRAFTED_FLOWER
        )))
    }

    @Test
    fun berryTastyRaisesAllD8sByTwoWithoutTouchingOtherSizes() {
        val d6 = FixedEffectDie(6, 2)
        val d8a = FixedEffectDie(8, 3)
        val d8b = FixedEffectDie(8, 7)
        val d8c = FixedEffectDie(8, 8)
        val actor = EffectTestFixture.player(1, listOf(d6, d8a, d8b, d8c))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val effect = GameEffect.RAISE_ALL_D8S_PLUS_2
        engine.execute(EffectTestFixture.request(game, actor, effect))
        assertEquals(listOf(2, 5, 8, 8), listOf(d6.value, d8a.value, d8b.value, d8c.value))
    }

    @Test
    fun bloomBackboneRaisesEveryEligibleDieByCurrentVineAndFlowerCount() {
        val d4 = FixedEffectDie(4, 3)
        val d8 = FixedEffectDie(8, 2)
        val actor = EffectTestFixture.player(1, listOf(d4, d8))
        graft(actor, PlantType.ROOT, "Root")
        graft(actor, PlantType.VINE, "Vine")
        val flower = graft(actor, PlantType.FLOWER, "Bloom Backbone")
        actor.creature.faceUp(flower.id)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val effect = GameEffect.RAISE_ALL_DICE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER
        engine.execute(EffectTestFixture.request(game, actor, effect))
        assertEquals(4, d4.value)
        assertEquals(4, d8.value)
    }

    @Test
    fun berryTastyCannotActWhenItsOnlyD8IsInALockedBattleRow() {
        val lockedD8 = FixedEffectDie(8, 2)
        val openD10 = FixedEffectDie(10, 3)
        val actor = EffectTestFixture.player(1, listOf(lockedD8, openD10))
        val other = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, other)
        val battle = BattleState(listOf(actor, other))
        battle.grid.placeDie(actor, StrikeRow.TOP, lockedD8)
        battle.grid.placeDie(actor, StrikeRow.MIDDLE, openD10)
        battle.grid.closeRow(StrikeRow.TOP)
        val request = EffectTestFixture.request(game, actor, GameEffect.RAISE_ALL_D8S_PLUS_2)
            .copy(phase = GameEffectPhase.BATTLE, battleState = battle)

        assertFalse(engine.canExecute(request))
        assertEquals(2, lockedD8.value)
    }

    @Test
    fun revisedRaisesLeaveDiceInAlreadyClosedBattleRowsUntouched() {
        val closedD8 = FixedEffectDie(8, 2)
        val openD8 = FixedEffectDie(8, 3)
        val openD10 = FixedEffectDie(10, 4)
        val actor = EffectTestFixture.player(1, listOf(closedD8, openD8, openD10))
        graft(actor, PlantType.VINE, "Vine")
        graft(actor, PlantType.FLOWER, "Flower")
        val opponent = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, opponent)
        val battle = BattleState(listOf(actor, opponent))
        battle.grid.placeDie(actor, StrikeRow.TOP, closedD8)
        battle.grid.placeDie(actor, StrikeRow.MIDDLE, openD8)
        battle.grid.placeDie(actor, StrikeRow.MIDDLE, openD10)
        battle.grid.closeRow(StrikeRow.TOP)

        val berry = EffectTestFixture.request(game, actor, GameEffect.RAISE_ALL_D8S_PLUS_2)
            .copy(phase = GameEffectPhase.BATTLE, battleState = battle)
        engine.execute(berry)
        assertEquals(2, closedD8.value)
        assertEquals(5, openD8.value)

        val backbone = EffectTestFixture.request(game, actor,
            GameEffect.RAISE_ALL_DICE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER)
            .copy(phase = GameEffectPhase.BATTLE, battleState = battle)
        engine.execute(backbone)
        assertEquals(2, closedD8.value)
        assertEquals(7, openD8.value)
        assertEquals(6, openD10.value)
    }

    private fun graft(actor: Player, type: PlantType, name: String): dugsolutions.leaf.v35.player.creature.CreatureCard {
        val card = PlantCard(
            quantity = 1,
            name = name,
            title = name,
            type = type,
            cost = when (type) { PlantType.ROOT -> 5; PlantType.VINE -> 7; PlantType.FLOWER -> 11 },
            lineIcon = null,
            vpIcon = "",
            typeIcon = "",
            fgColor = "",
            textColor = "",
            fullImage = "",
            backgroundImage = "",
            cardBackgroundImage = "",
            effect = GameEffect.UNKNOWN
        )
        return actor.creature.graft(card, actor.creature.legalPlacements(card).first())
    }
}
