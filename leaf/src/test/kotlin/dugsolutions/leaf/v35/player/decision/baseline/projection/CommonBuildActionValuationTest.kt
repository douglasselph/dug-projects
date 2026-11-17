package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.context.PlantStackView
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommonBuildActionValuationTest {
    private val draw = CultivationAction.Main(CultivationMainAction.Draw)
    private val roundOne = CultivationAction.Main(CultivationMainAction.RoundEffect1)
    private val roundTwo = CultivationAction.Main(CultivationMainAction.RoundEffect2)
    private val noFuture = emptyList<RoundCardType>()
    private val battleNext = listOf(RoundCardType.BATTLE)

    @Test fun `Root Four More and a D8 Draw compare by realized pips without legacy bases`() {
        val root = plant(GameEffect.RAISE_DIE_PLUS_4, "Root_05_02")
        val activate = CultivationAction.Main(CultivationMainAction.ActivatePlant(root))
        val context = context(hand = listOf(DieView(0, 8, 1)), supply = listOf(DieView(0, 8, 2)))
        val result = CommonBuildActionValuation.inspect(request(context, listOf(draw, activate)))
        assertEquals(4.5, result.actions[0].totalPipEquivalentChange!!, 1e-12)
        assertEquals(4.0, result.actions[1].totalPipEquivalentChange!!, 1e-12)
        assertEquals(0, result.actions[1].targetHandIndex)
        assertEquals(activate, result.actions[1].choice)
        assertEquals(activate, result.pipOnlyOrder[1].choice)
        assertFalse(result.readyToChoose) // Buy thresholds and Plant Refresh are NOT priced.
        assertTrue(result.actions[1].explanation.contains("NOT PRICED"))
    }

    @Test fun `capped Raise is zero rather than an arbitrary activation base`() {
        val root = plant(GameEffect.RAISE_DIE_PLUS_4, "Root_05_02")
        val activate = CultivationAction.Main(CultivationMainAction.ActivatePlant(root))
        val result = CommonBuildActionValuation.inspect(request(context(
            hand = listOf(DieView(0, 8, 8)), supply = listOf(DieView(0, 8, 1))
        ), listOf(draw, activate)))
        assertEquals(0.0, result.actions[1].totalPipEquivalentChange!!, 1e-12)
        assertEquals(4.5, result.actions[0].totalPipEquivalentChange!!, 1e-12)
    }

    @Test fun `early D12 Compost into D20 values the future four-mean-pip improvement`() {
        val context = context(
            hand = listOf(DieView(0, 12, 1)),
            supply = listOf(DieView(0, 4, 3)),
            futureRounds = battleNext,
            graftBed = mapOf(DieSides.D20 to 1)
        )
        val result = CommonBuildActionValuation.inspect(request(context, listOf(draw, roundOne),
            first = GameEffect.UPGRADE_DIE_FROM_HAND))
        val compost = result.actions[1]
        assertEquals(BuildValueCoverage.PARTIAL, compost.coverage)
        assertEquals(0, compost.targetHandIndex)
        assertEquals(-1.0, compost.immediate!!.spendingPips, 1e-12)
        assertEquals(4.0, compost.futureSpendingPipChange!!, 1e-12)
        assertEquals(0.0, compost.futureRewardEquivalentChange!!, 1e-12)
        assertEquals(3.0, compost.totalPipEquivalentChange!!, 1e-12)
        assertEquals(2.5, result.actions[0].immediate!!.spendingPips, 1e-12)
        assertEquals(roundOne, result.pipOnlyOrder.first().choice)
        assertFalse(result.readyToChoose)
    }

    @Test fun `late Compost still loses a showing die when no future rounds exist`() {
        val context = context(
            hand = listOf(DieView(0, 12, 1)), supply = listOf(DieView(0, 4, 3)),
            futureRounds = noFuture, graftBed = mapOf(DieSides.D20 to 1)
        )
        val result = CommonBuildActionValuation.inspect(request(context, listOf(draw, roundOne),
            first = GameEffect.UPGRADE_DIE_FROM_HAND))
        assertEquals(-1.0, result.actions[1].totalPipEquivalentChange!!, 1e-12)
        assertEquals(0.0, result.actions[1].futureSpendingPipChange!!, 1e-12)
        assertEquals(draw, result.pipOnlyOrder.first().choice)
    }

    @Test fun `drawing an owned D10 now is not counted twice as future value`() {
        val context = context(
            hand = listOf(DieView(0, 4, 1)),
            supply = listOf(DieView(0, 10, 5)),
            futureRounds = battleNext
        )
        val before = context.self.board
        val result = CommonBuildActionValuation.inspect(request(context, listOf(draw)))
        assertEquals(5.5, result.actions.single().immediate!!.spendingPips, 1e-12)
        assertEquals(0.0, result.actions.single().futureSpendingPipChange!!, 1e-12)
        assertEquals(5.5, result.actions.single().totalPipEquivalentChange!!, 1e-12)
        assertEquals(before, context.self.board)
    }

    @Test fun `Queen's Blossom draws two actual next-side dice on one Main action`() {
        val queen = plant(GameEffect.DRAW_TWO_DICE, "Flower_17_04")
        val activate = CultivationAction.Main(CultivationMainAction.ActivatePlant(queen))
        val context = context(supply = listOf(DieView(0, 10, 1), DieView(1, 4, 1)))
        val result = CommonBuildActionValuation.inspect(request(context, listOf(draw, activate)))
        assertEquals(2.5, result.actions[0].immediate!!.spendingPips, 1e-12)
        assertEquals(8.0, result.actions[1].immediate!!.spendingPips, 1e-12)
        assertEquals(activate, result.pipOnlyOrder.first().choice)
    }

    @Test fun `Roll Reward equivalents remain separate from actual Buy pips`() {
        val context = context(supply = listOf(DieView(0, 10, 2))).copy(
            grove = DecisionContext.EMPTY.grove.copy(bees = 1, wispDeckRemaining = 1)
        )
        val result = CommonBuildActionValuation.inspect(request(context, listOf(draw))).actions.single()
        val immediate = requireNotNull(result.immediate)
        assertEquals(5.5, immediate.spendingPips, 1e-12)
        assertEquals(0.2, immediate.critterOptionValue, 1e-12)
        assertEquals(0.3, immediate.wispOptionValue, 1e-12)
        assertEquals(6.0, result.totalPipEquivalentChange!!, 1e-12)
    }

    @Test fun `Compost without a Graft Bed step reports unsupported Trash fallback`() {
        val context = context(hand = listOf(DieView(0, 12, 2)))
        val value = CommonBuildActionValuation.inspect(request(context, listOf(roundOne),
            first = GameEffect.UPGRADE_DIE_FROM_HAND)).actions.single()
        assertEquals(BuildValueCoverage.UNMODELED, value.coverage)
        assertNull(value.totalPipEquivalentChange)
        assertTrue(value.explanation.contains("Trash"))
    }

    @Test fun `Water Sunlight and Mulch are never treated as worth zero`() {
        val context = context(hand = listOf(DieView(0, 8, 3)))
        val effects = listOf(GameEffect.GAIN_WATER_TOKEN, GameEffect.GAIN_SUNLIGHT_TOKEN,
            GameEffect.MULCH_DIE_FROM_HAND)
        effects.forEach { effect ->
            val result = CommonBuildActionValuation.inspect(request(context, listOf(roundOne), first = effect))
            assertNull(result.actions.single().totalPipEquivalentChange)
            assertEquals(BuildValueCoverage.UNMODELED, result.actions.single().coverage)
            assertTrue(result.actions.single().unmodeledConsequences.isNotEmpty())
        }
    }

    @Test fun `only actually legal Main offers are projected and both Round slots are supported`() {
        val context = context(hand = listOf(DieView(0, 8, 2)), supply = listOf(DieView(0, 6, 3)))
        val result = CommonBuildActionValuation.inspect(request(context, listOf(roundTwo),
            first = GameEffect.GAIN_WATER_TOKEN, second = GameEffect.RAISE_DIE_PLUS_4))
        assertEquals(listOf(roundTwo), result.actions.map { it.choice })
        assertEquals(4.0, result.actions.single().immediate!!.spendingPips, 1e-12)
    }

    @Test fun `possible Buy threshold crossing is reported but not awarded arbitrary points`() {
        val starting = context(hand = listOf(DieView(0, 8, 3)), supply = listOf(DieView(0, 4, 1)))
        val rootStack = PlantStackView("Root_05_01", "Test Root", PlantType.ROOT,
            5, GameEffect.DOUBLE_ONE_DIE, PlantScoringRule.Fixed(1), 1)
        val context = starting.copy(grove = starting.grove.copy(plantStacks = listOf(rootStack)))
        val result = CommonBuildActionValuation.inspect(request(context, listOf(draw))).actions.single()
        assertEquals(2.5, result.totalPipEquivalentChange!!, 1e-12)
        assertEquals(listOf(5), result.currentBuyThresholdsPossiblyCrossed)
        assertEquals(BuildValueCoverage.PARTIAL, result.coverage)
    }

    private fun context(
        supply: List<DieView> = emptyList(),
        hand: List<DieView> = emptyList(),
        discard: List<DieView> = emptyList(),
        futureRounds: List<RoundCardType> = noFuture,
        graftBed: Map<DieSides, Int> = emptyMap()
    ): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        progress = DecisionContext.EMPTY.progress.copy(upcomingRoundTypes = futureRounds),
        self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
            supply = supply, hand = hand, discard = discard
        )),
        grove = DecisionContext.EMPTY.grove.copy(graftBed = graftBed)
    )

    private fun request(
        context: DecisionContext,
        choices: List<CultivationAction.Main>,
        first: GameEffect = GameEffect.GAIN_ONE_VP,
        second: GameEffect = GameEffect.GAIN_ONE_VP
    ): ChooseCultivationActionRequest = ChooseCultivationActionRequest(
        roundCard = RoundCard(1, "Test Round", RoundCardType.CULTIVATION,
            RoundCardEffect("First", "", "", "", null, first),
            RoundCardEffect("Second", "", "", "", null, second), ""),
        mainActionsRemaining = 2,
        legalChoices = choices,
        context = context
    )

    private fun plant(effect: GameEffect, name: String): CreatureCard = CreatureCard(
        id = CreatureCardId(1),
        card = PlantCard(1, name, name, PlantType.ROOT, 5, null,
            "", "", "", "", "", "", "", effect, PlantScoringRule.Fixed(1)),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-1, 0),
        facing = CreatureCard.Facing.FACE_UP
    )
}
