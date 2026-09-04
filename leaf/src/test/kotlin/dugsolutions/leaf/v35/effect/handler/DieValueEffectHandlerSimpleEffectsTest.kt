package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.LastEffectChoiceStrategy
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DieValueEffectHandlerSimpleEffectsTest {

    private val handler = DieValueEffectHandler()
    private val nested = GameEffectExecutor { }

    @Test
    fun simpleRaiseDoubleAndSetEffectsMutateChosenHandDie() {
        val first = FixedEffectDie(6, 2)
        val second = FixedEffectDie(8, 4)
        val actor = EffectTestFixture.player(
            1,
            hand = listOf(first, second),
            effectStrategy = LastEffectChoiceStrategy()
        )
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(game, actor, GameEffect.DOUBLE_ONE_DIE),
            nested
        )
        assertEquals(2, first.value)
        assertEquals(8, second.value)

        second.adjustTo(4)
        handler.execute(
            EffectTestFixture.request(game, actor, GameEffect.RAISE_DIE_PLUS_4),
            nested
        )
        assertEquals(8, second.value)

        second.adjustTo(2)
        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3
            ),
            nested
        )
        assertEquals(3, second.value)
    }

    @Test
    fun flipDoesNotOfferD4() {
        val d4 = FixedEffectDie(4, 2)
        val d8 = FixedEffectDie(8, 2)
        val actor = EffectTestFixture.player(
            1,
            hand = listOf(d4, d8)
        )
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE
        )

        assertTrue(handler.canExecute(request))
        handler.execute(request, nested)

        assertEquals(2, d4.value)
        assertEquals(7, d8.value)
    }

    @Test
    fun downPaymentSetsChosenDieToOneThenScoresAllOwnOnes() {
        val alreadyOne = FixedEffectDie(4, 1)
        val chosen = FixedEffectDie(8, 5)
        val actor = EffectTestFixture.player(
            1,
            hand = listOf(alreadyOne, chosen),
            effectStrategy = LastEffectChoiceStrategy()
        )
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE
            ),
            nested
        )

        assertEquals(1, chosen.value)
        assertEquals(2, actor.vp)
    }

    @Test
    fun lowestValueToMaxOffersOnlyLowestDice() {
        val low = FixedEffectDie(6, 2)
        val high = FixedEffectDie(8, 5)
        val actor = EffectTestFixture.player(
            1,
            hand = listOf(low, high),
            effectStrategy = LastEffectChoiceStrategy()
        )
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(game, actor, GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX),
            nested
        )

        assertEquals(6, low.value)
        assertEquals(5, high.value)
    }

    @Test
    fun setUpToD12ToMaxNeverOffersD20() {
        val d20 = FixedEffectDie(20, 3)
        val d12 = FixedEffectDie(12, 4)
        val actor = EffectTestFixture.player(
            1,
            hand = listOf(d20, d12),
            effectStrategy = LastEffectChoiceStrategy()
        )
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(game, actor, GameEffect.SET_DIE_UP_TO_D12_TO_MAX),
            nested
        )

        assertEquals(3, d20.value)
        assertEquals(12, d12.value)
    }

    @Test
    fun raiseAllDicePlus2RaisesEveryHandDieIndependently() {
        val d4 = FixedEffectDie(4, 3)
        val d10 = FixedEffectDie(10, 4)
        val actor = EffectTestFixture.player(1, hand = listOf(d4, d10))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(game, actor, GameEffect.RAISE_ALL_DICE_PLUS_2),
            nested
        )

        assertEquals(4, d4.value)
        assertEquals(6, d10.value)
    }

    @Test
    fun sappingSnapdragonSupportsCultivationAndBattleWhenBattleDieIsPlaced() {
        val die = FixedEffectDie(8, 3)
        val actor = EffectTestFixture.player(1, hand = listOf(die))
        val other = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, other)
        val cultivation = EffectTestFixture.request(
            game,
            actor,
            GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW
        )
        val battleState = dugsolutions.leaf.v35.battle.BattleState(
            listOf(actor, other)
        )
        battleState.grid.placeDie(
            actor,
            dugsolutions.leaf.v35.battle.domain.StrikeRow.TOP,
            die
        )
        val battle = cultivation.copy(
            phase = GameEffectPhase.BATTLE,
            battleState = battleState
        )

        assertTrue(handler.canExecute(cultivation))
        assertTrue(handler.canExecute(battle))
    }

    @Test
    fun graftCountRaisesOnePointPerMatchingCard() {
        val die = FixedEffectDie(12, 2)
        val actor = EffectTestFixture.player(1, hand = listOf(die))
        actor.creature.graft(
            plant("Vine", PlantType.VINE),
            GraftPlacement(CreatureSide.LEFT, CreaturePosition(-1, 0))
        )
        actor.creature.graft(
            plant("Flower", PlantType.FLOWER),
            GraftPlacement(CreatureSide.LEFT, CreaturePosition(-1, 1))
        )
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER
            ),
            nested
        )

        assertEquals(4, die.value)
    }

    private fun plant(name: String, type: PlantType) = PlantCard(
        quantity = 1,
        name = name,
        title = name,
        type = type,
        cost = 5,
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
}
