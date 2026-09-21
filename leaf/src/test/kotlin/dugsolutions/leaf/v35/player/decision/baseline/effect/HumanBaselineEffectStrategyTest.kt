package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDiceRequest
import dugsolutions.leaf.v35.player.decision.effect.ChoosePetalToDie4Request
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.PetalToDie4Choice
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HumanBaselineEffectStrategyTest {

    @Nested
    inner class `Cultivation action target alignment` {

        @Test
        fun `Sunlight chooses the die that realizes the best visible plus three gain`() {
            val context = context(
                hand = listOf(
                    DieView(index = 0, sides = 6, value = 5),
                    DieView(index = 1, sides = 10, value = 7)
                )
            )

            val chosen = HumanBaselineEffectStrategy().chooseDie(
                ChooseEffectDieRequest(
                    effect = GameEffect.RAISE_DIE_PLUS_3,
                    legalChoices = effectChoices(context),
                    context = context
                )
            )

            assertEquals(1, chosen.index)
        }

        @Test
        fun `Mulch chooses the poor roll whose future stored value justified the action`() {
            val context = context(
                hand = listOf(
                    DieView(index = 0, sides = 12, value = 1),
                    DieView(index = 1, sides = 20, value = 10)
                )
            )

            val chosen = HumanBaselineEffectStrategy().chooseDie(
                ChooseEffectDieRequest(
                    effect = GameEffect.MULCH_DIE_FROM_HAND,
                    legalChoices = effectChoices(context),
                    context = context
                )
            )

            assertEquals(0, chosen.index)
        }

        @Test
        fun `Compost chooses only a legal normal step and respects current Buy impact`() {
            val context = context(
                hand = listOf(
                    DieView(index = 0, sides = 4, value = 1),
                    DieView(index = 1, sides = 6, value = 2)
                ),
                graftBed = mapOf(
                    // D4 cannot skip the missing D6. D6 -> D8 is the only
                    // legal normal one-step upgrade represented here.
                    DieSides.D8 to 1
                )
            )

            val chosen = HumanBaselineEffectStrategy().chooseDie(
                ChooseEffectDieRequest(
                    effect = GameEffect.UPGRADE_DIE_FROM_HAND,
                    legalChoices = listOf(EffectDieChoice(index = 1, sides = 6, value = 2)),
                    context = context
                )
            )

            assertEquals(1, chosen.index)
        }

        @Test
        fun `Petal To Die 4 chooses the same high-value trash branch anticipated by card scoring`() {
            val d4 = EffectDieChoice(index = 0, sides = 4, value = 1)
            val context = context(
                hand = listOf(
                    DieView(index = 0, sides = 4, value = 1),
                    DieView(index = 1, sides = 20, value = 1),
                    DieView(index = 2, sides = 12, value = 2),
                    DieView(index = 3, sides = 10, value = 1)
                ),
                graftBed = mapOf(DieSides.D4 to 1)
            )

            val chosen = HumanBaselineEffectStrategy().choosePetalToDie4(
                ChoosePetalToDie4Request(
                    effect = GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4,
                    legalChoices = listOf(
                        PetalToDie4Choice.GainD4,
                        PetalToDie4Choice.TrashD4AndRaiseAll(d4)
                    ),
                    context = context
                )
            )

            assertIs<PetalToDie4Choice.TrashD4AndRaiseAll>(chosen)
        }

        @Test
        fun `Petal To Die 4 keeps the gain branch when raising the remaining dice has little value`() {
            val d4 = EffectDieChoice(index = 0, sides = 4, value = 4)
            val context = context(
                hand = listOf(
                    DieView(index = 0, sides = 4, value = 4),
                    DieView(index = 1, sides = 6, value = 6),
                    DieView(index = 2, sides = 8, value = 8)
                ),
                graftBed = mapOf(DieSides.D4 to 1)
            )

            val chosen = HumanBaselineEffectStrategy().choosePetalToDie4(
                ChoosePetalToDie4Request(
                    effect = GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4,
                    legalChoices = listOf(
                        PetalToDie4Choice.GainD4,
                        PetalToDie4Choice.TrashD4AndRaiseAll(d4)
                    ),
                    context = context
                )
            )

            assertEquals(PetalToDie4Choice.GainD4, chosen)
        }
    }

    @Test
    fun `Root Cause prefers a D20 showing one over a mediocre D6`() {
        val context = context(
            hand = listOf(
                DieView(index = 0, sides = 6, value = 3),
                DieView(index = 1, sides = 20, value = 1)
            )
        )
        val strategy = HumanBaselineEffectStrategy()
        val chosen = strategy.chooseDie(
            ChooseEffectDieRequest(
                effect = GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE,
                legalChoices = listOf(
                    EffectDieChoice(index = 0, sides = 6, value = 3),
                    EffectDieChoice(index = 1, sides = 20, value = 1)
                ),
                context = context
            )
        )
        assertEquals(1, chosen.index)
    }

    @Test
    fun `Root Recall scores complete legal subsets instead of greedily keeping a fixed count`() {
        val context = context(
            hand = listOf(
                DieView(index = 0, sides = 20, value = 1),
                DieView(index = 1, sides = 6, value = 5),
                DieView(index = 2, sides = 8, value = 2)
            )
        )
        val strategy = HumanBaselineEffectStrategy()
        val chosen = strategy.chooseDice(
            ChooseEffectDiceRequest(
                effect = GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE,
                legalChoices = listOf(
                    EffectDieChoice(0, 20, 1),
                    EffectDieChoice(1, 6, 5),
                    EffectDieChoice(2, 8, 2)
                ),
                minChoices = 0,
                maxChoices = 3,
                context = context
            )
        )

        assertEquals(setOf(0, 2), chosen.selected.map { it.index }.toSet())
    }

    private fun context(
        hand: List<DieView>,
        graftBed: Map<DieSides, Int> = emptyMap()
    ): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(hand = hand)
        ),
        grove = DecisionContext.EMPTY.grove.copy(graftBed = graftBed)
    )

    private fun effectChoices(context: DecisionContext): List<EffectDieChoice> =
        context.self.board.hand.map { EffectDieChoice(it.index, it.sides, it.value) }
}
