package dugsolutions.leaf.v35.player.decision.effect

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BaselineEffectStrategyTest {

    @Test
    fun chooseDie_returnsFirstLegalChoice() {
        val first = EffectDieChoice(0, 6, 3)
        val second = EffectDieChoice(1, 8, 5)

        val chosen = BaselineEffectStrategy().chooseDie(
            ChooseEffectDieRequest(
                effect = GameEffect.RAISE_DIE_PLUS_3,
                legalChoices = listOf(first, second)
            )
        )

        assertEquals(first, chosen)
    }

    @Test
    fun chooseOptionalDie_returnsFirstChoiceOrNull() {
        val first = EffectDieChoice(0, 6, 3)
        val strategy = BaselineEffectStrategy()

        assertEquals(
            first,
            strategy.chooseOptionalDie(
                ChooseOptionalEffectDieRequest(
                    effect = GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN,
                    legalChoices = listOf(first)
                )
            )
        )

        assertNull(
            strategy.chooseOptionalDie(
                ChooseOptionalEffectDieRequest(
                    effect = GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN,
                    legalChoices = emptyList()
                )
            )
        )
    }


    @Test
    fun combinedTargetChoices_areExplicitAndDeterministic() {
        val first = EffectDieChoice(0, 6, 3)
        val second = EffectDieChoice(1, 8, 5)
        val strategy = BaselineEffectStrategy()

        assertEquals(
            EffectDiceChoice(listOf(first)),
            strategy.chooseDice(
                ChooseEffectDiceRequest(
                    effect = GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE,
                    legalChoices = listOf(first, second)
                )
            )
        )

        val pair = EffectDiePairChoice(source = first, target = second)
        assertEquals(
            pair,
            strategy.chooseDiePair(
                ChooseEffectDiePairRequest(
                    effect = GameEffect.SET_DIE_TO_MATCH_ANOTHER,
                    legalChoices = listOf(pair)
                )
            )
        )

        val critterDie = EffectCritterDieChoice(Critter.WORM, second)
        assertEquals(
            critterDie,
            strategy.chooseCritterAndDie(
                ChooseEffectCritterDieRequest(
                    effect = GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5,
                    legalChoices = listOf(critterDie)
                )
            )
        )
    }

    @Test
    fun requests_defensivelyCopyLegalChoices() {
        val mutable = mutableListOf(
            EffectDieChoice(0, 6, 3)
        )
        val required = ChooseEffectDieRequest(
            effect = GameEffect.RAISE_DIE_PLUS_3,
            legalChoices = mutable
        )
        val optional = ChooseOptionalEffectDieRequest(
            effect = GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN,
            legalChoices = mutable
        )
        val many = ChooseEffectDiceRequest(
            effect = GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE,
            legalChoices = mutable
        )

        mutable.clear()

        assertEquals(1, required.legalChoices.size)
        assertEquals(1, optional.legalChoices.size)
        assertEquals(1, many.legalChoices.size)
    }
}
