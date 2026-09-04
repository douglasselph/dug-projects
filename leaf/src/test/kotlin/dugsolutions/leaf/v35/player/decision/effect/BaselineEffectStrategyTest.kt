package dugsolutions.leaf.v35.player.decision.effect

import dugsolutions.leaf.v35.effect.GameEffect
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

        mutable.clear()

        assertEquals(1, required.legalChoices.size)
        assertEquals(1, optional.legalChoices.size)
    }
}
