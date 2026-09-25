package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.ButterflyView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import dugsolutions.leaf.v35.player.decision.effect.ChooseBeeSourceRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectButterflyTargetRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectCritterDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectBeeSourceChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectButterflyTargetChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectCritterDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineEffectStrategyResourceChoiceTest {
    @Test
    fun `outside Battle Bee source steals from an opponent rather than consuming Grove Bee`() {
        val opponent = PlayerId(2)
        val chosen = HumanBaselineEffectStrategy().chooseBeeSource(
            ChooseBeeSourceRequest(
                effect = GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND,
                legalChoices = listOf(
                    EffectBeeSourceChoice.Grove,
                    EffectBeeSourceChoice.Opponent(opponent)
                ),
                context = DecisionContext.EMPTY.copy(
                    opponents = listOf(opponent(opponent))
                )
            )
        )

        assertEquals(EffectBeeSourceChoice.Opponent(opponent), chosen)
    }

    @Test
    fun `Butterfly source prefers taking a face-up Butterfly from an opponent over Grove`() {
        val opponent = PlayerId(2)
        val chosen = HumanBaselineEffectStrategy().chooseButterflyTarget(
            ChooseEffectButterflyTargetRequest(
                effect = GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES,
                legalChoices = listOf(
                    EffectButterflyTargetChoice(null, Butterfly.GREEN),
                    EffectButterflyTargetChoice(opponent, Butterfly.YELLOW)
                ),
                context = DecisionContext.EMPTY.copy(
                    opponents = listOf(
                        opponent(
                            opponent,
                            butterflies = listOf(ButterflyView(Butterfly.YELLOW, isFaceUp = true))
                        )
                    )
                )
            )
        )

        assertEquals(EffectButterflyTargetChoice(opponent, Butterfly.YELLOW), chosen)
    }

    @Test
    fun `Vine and Dine spends the Critter with more room above its protected reserve`() {
        val die = EffectDieChoice(index = 0, sides = 12, value = 4)
        val chosen = HumanBaselineEffectStrategy().chooseCritterAndDie(
            ChooseEffectCritterDieRequest(
                effect = GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5,
                legalChoices = listOf(
                    EffectCritterDieChoice(Critter.BEE, die),
                    EffectCritterDieChoice(Critter.WORM, die)
                ),
                context = DecisionContext.EMPTY.copy(
                    self = DecisionContext.EMPTY.self.copy(
                        board = DecisionContext.EMPTY.self.board.copy(bees = 2, worms = 2)
                    )
                )
            )
        )

        // Two Bees are exactly the protected Bee reserve; two Worms leave one
        // Worm above the protected Worm reserve. Spend the less-scarce Worm.
        assertEquals(EffectCritterDieChoice(Critter.WORM, die), chosen)
    }

    @Test
    fun `Vine and Dine protects the last Worm more strongly than a Bee above Bee reserve`() {
        val die = EffectDieChoice(index = 0, sides = 12, value = 4)
        val chosen = HumanBaselineEffectStrategy().chooseCritterAndDie(
            ChooseEffectCritterDieRequest(
                effect = GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5,
                legalChoices = listOf(
                    EffectCritterDieChoice(Critter.WORM, die),
                    EffectCritterDieChoice(Critter.BEE, die)
                ),
                context = DecisionContext.EMPTY.copy(
                    self = DecisionContext.EMPTY.self.copy(
                        board = DecisionContext.EMPTY.self.board.copy(bees = 3, worms = 1)
                    )
                )
            )
        )

        assertEquals(EffectCritterDieChoice(Critter.BEE, die), chosen)
    }

    private fun opponent(
        id: PlayerId,
        butterflies: List<ButterflyView> = emptyList()
    ): OpponentView = OpponentView(
        board = DecisionContext.EMPTY.self.board.copy(id = id, butterflies = butterflies),
        wispCount = 0
    )
}
