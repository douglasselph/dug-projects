package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import dugsolutions.leaf.v35.player.decision.baseline.card.CardScoringHelpers
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.HumanBaselineCultivationStrategy
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.context.GroveView
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CultivationMainPriorityTest {

    @Test
    fun `Compost applies the supplied modest dice-development nudge`() {
        val context = context(
            hand = listOf(DieView(index = 0, sides = 4, value = 2)),
            grove = DecisionContext.EMPTY.grove.copy(
                graftBed = mapOf(DieSides.D6 to 1)
            )
        )

        val withoutNudge = CompostPriority.score(
            context = context,
            normalPurchasingPower = 2,
            developmentBonus = 0
        )
        val withNudge = CompostPriority.score(
            context = context,
            normalPurchasingPower = 2,
            developmentBonus = 9
        )

        assertEquals(withoutNudge.total + 9, withNudge.total)
        assertTrue(withNudge.adjustments.any {
            it.amount == 9 && it.reason == "Dice development is behind target"
        })
    }

    @Test
    fun `Compost applies a strong guardrail when it would leave less than five Buy power`() {
        val context = context(
            hand = listOf(DieView(index = 0, sides = 4, value = 2)),
            grove = DecisionContext.EMPTY.grove.copy(
                graftBed = mapOf(DieSides.D6 to 1)
            )
        )

        val score = requireNotNull(
            CompostPriority.targetScore(
                context = context,
                die = context.self.board.hand.single(),
                normalPurchasingPower = 4
            )
        )

        assertTrue(score.adjustments.any {
            it.amount == -40 && it.reason == "Preserve at least 5 Hand-die Buy power"
        })
    }

    @Test
    fun `Compost use percentage favors low rolls and makes repeated die commitments rarer`() {
        val lowRoll = context(
            hand = listOf(
                DieView(index = 0, sides = 4, value = 2),
                DieView(index = 1, sides = 20, value = 5)
            ),
            grove = DecisionContext.EMPTY.grove.copy(graftBed = mapOf(DieSides.D6 to 1))
        )
        val ordinaryRoll = context(
            hand = listOf(
                DieView(index = 0, sides = 4, value = 3),
                DieView(index = 1, sides = 20, value = 5)
            ),
            grove = DecisionContext.EMPTY.grove.copy(graftBed = mapOf(DieSides.D6 to 1))
        )
        val belowBuyFloor = context(
            hand = listOf(
                DieView(index = 0, sides = 4, value = 2),
                DieView(index = 1, sides = 20, value = 2)
            ),
            grove = DecisionContext.EMPTY.grove.copy(graftBed = mapOf(DieSides.D6 to 1))
        )

        assertEquals(75, CompostPriority.usePercentage(lowRoll, 7, mainActionsRemaining = 2))
        assertEquals(50, CompostPriority.usePercentage(ordinaryRoll, 8, mainActionsRemaining = 2))
        assertEquals(10, CompostPriority.usePercentage(belowBuyFloor, 4, mainActionsRemaining = 2))
        assertEquals(37, CompostPriority.usePercentage(lowRoll, 7, mainActionsRemaining = 1))
    }

    @Test
    fun `Mulch threshold scoring uses supplied normal purchasing power`() {
        val context = context(
            hand = listOf(DieView(index = 0, sides = 10, value = 7)),
            grove = DecisionContext.EMPTY.grove.copy(
                graftBed = mapOf(DieSides.D10 to 1)
            )
        )

        val protectedCrittersExcluded = MulchPriority.score(
            context = context,
            normalPurchasingPower = 7
        )
        val hypotheticalAllCrittersSpendable = MulchPriority.score(
            context = context,
            normalPurchasingPower = 12
        )

        // At power 7, removing the die does not lose the D10 Buy tier because
        // that tier was not affordable. At power 12, removing it loses D10.
        assertEquals(
            protectedCrittersExcluded.total - 10,
            hypotheticalAllCrittersSpendable.total
        )
    }

    @Test
    fun `Plant activation threshold scoring can use reserve-aware purchasing power`() {
        val context = context(
            hand = listOf(DieView(index = 0, sides = 10, value = 5)),
            bees = 2,
            worms = 1,
            grove = DecisionContext.EMPTY.grove.copy(
                graftBed = mapOf(DieSides.D8 to 1)
            )
        )
        val reserveAwarePower = HumanBaselinePolicy().normalPurchasingPower(context)

        val reserveAware = CardScoringHelpers.playScore(
            context = context,
            phase = CardPhase.CULTIVATION,
            effect = GameEffect.RAISE_DIE_PLUS_4,
            cardName = "test",
            base = 0,
            normalPurchasingPower = reserveAwarePower
        )
        val legacyAllCrittersSpendable = CardScoringHelpers.playScore(
            context = context,
            phase = CardPhase.CULTIVATION,
            effect = GameEffect.RAISE_DIE_PLUS_4,
            cardName = "test",
            base = 0
        )

        // Hand power 5 + a visible +4 crosses D8. Counting the protected
        // 2 Bees + 1 Worm instead starts at 10 and hides that threshold gain.
        assertEquals(legacyAllCrittersSpendable.total + 10, reserveAware.total)
    }

    @Test
    fun `Sunlight Main choice respects policy purchasing power instead of protected Critters`() {
        val context = context(
            supply = listOf(DieView(index = 0, sides = 8, value = 1)),
            hand = listOf(DieView(index = 0, sides = 10, value = 7)),
            bees = 2,
            worms = 1,
            grove = DecisionContext.EMPTY.grove.copy(
                graftBed = mapOf(DieSides.D10 to 1)
            )
        )
        val request = ChooseCultivationActionRequest(
            roundCard = sunlightRound(),
            mainActionsRemaining = 2,
            legalChoices = listOf(
                CultivationAction.Main(CultivationMainAction.Draw),
                CultivationAction.Main(CultivationMainAction.RoundEffect1)
            ),
            context = context
        )

        val reserveAware = HumanBaselineCultivationStrategy().chooseAction(request)
        val allCrittersSpendable = HumanBaselineCultivationStrategy(
            policy = object : HumanBaselinePolicy() {
                override fun normalPurchasingPower(context: DecisionContext): Int = 12
            }
        ).chooseAction(request)

        // Reserve-aware power is 7, so +3 Sunlight crosses the D10 threshold:
        // Sunlight 60 beats D8 Draw 53. If protected Critters are treated as
        // ordinary purchasing power, the threshold bonus disappears and Draw wins.
        assertEquals(
            CultivationAction.Main(CultivationMainAction.RoundEffect1),
            reserveAware
        )
        assertEquals(
            CultivationAction.Main(CultivationMainAction.Draw),
            allCrittersSpendable
        )
    }

    @Test
    fun `Compost target scoring does not skip a missing normal Upgrade size`() {
        val context = context(
            hand = listOf(
                DieView(index = 0, sides = 4, value = 1),
                DieView(index = 1, sides = 6, value = 2)
            ),
            grove = DecisionContext.EMPTY.grove.copy(
                graftBed = mapOf(DieSides.D8 to 1)
            )
        )

        assertEquals(
            null,
            CompostPriority.targetScore(
                context = context,
                die = context.self.board.hand[0],
                normalPurchasingPower = 3
            )
        )
        assertTrue(
            CompostPriority.targetScore(
                context = context,
                die = context.self.board.hand[1],
                normalPurchasingPower = 3
            ) != null
        )
    }

    @Test
    fun `Sunlight action and target use the same target-specific value`() {
        val context = context(
            hand = listOf(
                DieView(index = 0, sides = 6, value = 5),
                DieView(index = 1, sides = 10, value = 7)
            )
        )

        val first = SunlightPriority.targetScore(context, context.self.board.hand[0], 12)
        val second = SunlightPriority.targetScore(context, context.self.board.hand[1], 12)
        val action = SunlightPriority.score(context, 12)

        assertTrue(second.total > first.total)
        assertEquals(35 + second.total, action.total)
    }

    private fun context(
        supply: List<DieView> = emptyList(),
        hand: List<DieView> = emptyList(),
        bees: Int = 0,
        worms: Int = 0,
        grove: GroveView = DecisionContext.EMPTY.grove
    ): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(
                supply = supply,
                hand = hand,
                bees = bees,
                worms = worms
            )
        ),
        grove = grove
    )

    private fun sunlightRound() = RoundCard(
        quantity = 1,
        name = "sunlight test",
        type = RoundCardType.CULTIVATION,
        firstEffect = RoundCardEffect("Sunlight", "", "", "", null, GameEffect.RAISE_DIE_PLUS_3),
        secondEffect = RoundCardEffect("VP", "", "", "", null, GameEffect.GAIN_ONE_VP),
        backImage = ""
    )
}
