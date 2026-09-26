package dugsolutions.leaf.v35.player.decision.baseline

import dugsolutions.leaf.v35.player.decision.baseline.common.ReserveResource
import dugsolutions.leaf.v35.player.decision.baseline.common.ResourceReserveTargets
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.context.GameProgressView
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HumanBaselinePolicyTest {

    @Test
    fun `default protected Critter reserve is centralized at two Bees and one Worm`() {
        val reserve = HumanBaselinePolicy().protectedCritterReserve(context())

        assertEquals(2, reserve.bees)
        assertEquals(1, reserve.worms)
    }

    @Test
    fun `default post-reserve Critter preference is two-thirds Bee`() {
        assertEquals(2.0 / 3.0, HumanBaselinePolicy().postReserveBeeProbability(context()))
    }

    @Test
    fun `constructor knob can customize post-reserve Bee probability`() {
        val policy = HumanBaselinePolicy(postReserveBeeProbabilityValue = 0.4)

        assertEquals(0.4, policy.postReserveBeeProbability(context()))
    }


    @Test
    fun `default Graft topology considers three future growth slots adequate`() {
        assertEquals(3, HumanBaselinePolicy().graftAdequateGrowthSlots(context()))
    }

    @Test
    fun `Graft adequate-growth threshold is configurable per policy`() {
        val policy = HumanBaselinePolicy(graftAdequateGrowthSlotsValue = 5)

        assertEquals(5, policy.graftAdequateGrowthSlots(context()))
    }


    @Test
    fun `default Cultivation support reserve combines Critters with one Water and one Mulch`() {
        val reserve = HumanBaselinePolicy().protectedCultivationResourceReserve(context())

        assertEquals(2, reserve.bees)
        assertEquals(1, reserve.worms)
        assertEquals(1, reserve.water)
        assertEquals(1, reserve.mulch)
    }

    @Test
    fun `normal purchasing power excludes protected Critters and includes only surplus`() {
        val context = context(
            hand = listOf(DieView(0, 6, 6)),
            bees = 3,
            worms = 2
        )

        // 6 dice + one surplus Bee worth 2 + one surplus Worm worth 1.
        assertEquals(9, HumanBaselinePolicy().normalPurchasingPower(context))
    }

    @Test
    fun `normal purchasing power honors an overridden reserve policy`() {
        val context = context(
            hand = listOf(DieView(0, 6, 6)),
            bees = 2,
            worms = 1
        )
        val policy = object : HumanBaselinePolicy() {
            override fun protectedCritterReserve(context: DecisionContext) =
                ResourceReserveTargets(bees = 1, worms = 0)
        }

        // The base implementation calls the overridable reserve method rather
        // than reading companion defaults directly: 6 + Bee(2) + Worm(1) = 9.
        assertEquals(9, policy.normalPurchasingPower(context))
    }

    @Test
    fun `Cultivation dice development bonus is a modest capped nudge`() {
        val policy = HumanBaselinePolicy()
        val context = context(
            progress = GameProgressView.EMPTY.copy(
                currentCultivationRoundNumber = 1,
                totalCultivationRounds = 8
            )
        )

        assertEquals(9, policy.cultivationDiceDevelopmentBonus(context))
    }

    @Test
    fun `constructor knobs can tune Cultivation policy without changing production constants`() {
        val policy = HumanBaselinePolicy(
            cultivationDiceDeficitMaxBonusValue = 5,
            cultivationDoneScoreValue = 63,
            cultivationReserveSpendPenaltyPerUnitValue = 12
        )
        val context = context(
            progress = GameProgressView.EMPTY.copy(currentCultivationRoundNumber = 1)
        )

        assertEquals(5, policy.cultivationDiceDevelopmentBonus(context))
        assertEquals(63, policy.cultivationDoneScore(context))
        assertEquals(12, policy.cultivationReserveSpendPenaltyPerUnit(context, ReserveResource.WATER))
    }



    @Test
    fun `default Overgrowth willingness grows with target die size`() {
        val policy = HumanBaselinePolicy()
        val context = context()

        assertEquals(5, policy.overgrowthUsePercentage(context, 4))
        assertEquals(10, policy.overgrowthUsePercentage(context, 6))
        assertEquals(20, policy.overgrowthUsePercentage(context, 8))
        assertEquals(40, policy.overgrowthUsePercentage(context, 10))
        assertEquals(0, policy.overgrowthUsePercentage(context, 12))
    }

    @Test
    fun `Overgrowth willingness is configurable per policy`() {
        val policy = HumanBaselinePolicy(
            overgrowthD4UsePercentageValue = 1,
            overgrowthD6UsePercentageValue = 2,
            overgrowthD8UsePercentageValue = 3,
            overgrowthD10UsePercentageValue = 90
        )
        val context = context()

        assertEquals(1, policy.overgrowthUsePercentage(context, 4))
        assertEquals(2, policy.overgrowthUsePercentage(context, 6))
        assertEquals(3, policy.overgrowthUsePercentage(context, 8))
        assertEquals(90, policy.overgrowthUsePercentage(context, 10))
    }

    @Test
    fun `default Sunlight willingness is fifty percent`() {
        assertEquals(50, HumanBaselinePolicy().sunlightUsePercentage(context()))
    }

    @Test
    fun `default Mulch willingness falls with showing value and stops at five`() {
        val policy = HumanBaselinePolicy()
        val context = context()

        assertEquals(80, policy.mulchUsePercentage(context, 1))
        assertEquals(60, policy.mulchUsePercentage(context, 2))
        assertEquals(40, policy.mulchUsePercentage(context, 3))
        assertEquals(20, policy.mulchUsePercentage(context, 4))
        assertEquals(0, policy.mulchUsePercentage(context, 5))
        assertEquals(0, policy.mulchUsePercentage(context, 6))
    }

    @Test
    fun `Pocketed Spark is conservative in Cultivation and much readier in Battle`() {
        val policy = HumanBaselinePolicy()
        val cultivation = context()
        val battle = cultivation.copy(phase = RoundCardType.BATTLE)

        assertEquals(0, policy.pocketedSparkUsePercentage(cultivation, 4))
        assertEquals(5, policy.pocketedSparkUsePercentage(cultivation, 6))
        assertEquals(10, policy.pocketedSparkUsePercentage(cultivation, 8))
        assertEquals(20, policy.pocketedSparkUsePercentage(cultivation, 10))
        assertEquals(50, policy.pocketedSparkUsePercentage(cultivation, 12))
        assertEquals(80, policy.pocketedSparkUsePercentage(cultivation, 20))

        assertEquals(5, policy.pocketedSparkUsePercentage(battle, 4))
        assertEquals(15, policy.pocketedSparkUsePercentage(battle, 6))
        assertEquals(30, policy.pocketedSparkUsePercentage(battle, 8))
        assertEquals(50, policy.pocketedSparkUsePercentage(battle, 10))
        assertEquals(75, policy.pocketedSparkUsePercentage(battle, 12))
        assertEquals(95, policy.pocketedSparkUsePercentage(battle, 20))
    }

    @Test
    fun `low Plant priority is ninety percent whenever Plant count is below two`() {
        val policy = HumanBaselinePolicy()

        assertEquals(90, policy.lowPlantPriorityPercentage(context()))
        assertEquals(
            90,
            policy.lowPlantPriorityPercentage(
                context(
                    plantCount = 1,
                    progress = GameProgressView.EMPTY.copy(
                        currentCultivationRoundNumber = 6,
                        battleRoundsCompleted = 2
                    )
                )
            )
        )
        assertEquals(
            0,
            policy.lowPlantPriorityPercentage(
                context(
                    plantCount = 2,
                    progress = GameProgressView.EMPTY.copy(
                        currentCultivationRoundNumber = 1,
                        battleRoundsCompleted = 0
                    )
                )
            )
        )
    }

    @Test
    fun `low Plant floor and percentage are configurable for player profiles`() {
        val novice = HumanBaselinePolicy(
            lowPlantPriorityPercentageValue = 25,
            lowPlantFloorValue = 1
        )

        assertEquals(25, novice.lowPlantPriorityPercentage(context()))
        assertEquals(0, novice.lowPlantPriorityPercentage(context(plantCount = 1)))
    }

    @Test
    fun `default Buy Plant tier curve makes cheapest standard tier about two percent`() {
        val policy = HumanBaselinePolicy()
        val weights = (0..5).map { policy.buyPlantCostTierWeight(context(), it) }
        val cheapestPercentage = weights.first().toDouble() / weights.sum() * 100.0

        assertTrue(cheapestPercentage in 1.9..2.1)
        assertTrue(weights.zipWithNext().all { (a, b) -> b > a })
        assertEquals(5, policy.buyCheaperPlantMinimumRemainingDice(context()))
    }

    @Test
    fun `one Bee die upgrade chance rises exponentially with die tier`() {
        val policy = HumanBaselinePolicy()
        val context = context()

        assertEquals(4, policy.buyBeeUpgradeDiePercentage(context, DieSides.D4, 1))
        assertEquals(11, policy.buyBeeUpgradeDiePercentage(context, DieSides.D6, 1))
        assertEquals(26, policy.buyBeeUpgradeDiePercentage(context, DieSides.D8, 1))
        assertEquals(50, policy.buyBeeUpgradeDiePercentage(context, DieSides.D10, 1))
        assertEquals(0, policy.buyBeeUpgradeDiePercentage(context, DieSides.D12, 1))
        assertEquals(0, policy.buyBeeUpgradeDiePercentage(context, DieSides.D20, 1))
    }

    @Test
    fun `additional Bees multiply Buy upgrade odds while low die use stays rarer`() {
        val policy = HumanBaselinePolicy()
        val context = context()

        assertEquals(14, policy.buyBeeUpgradeDiePercentage(context, DieSides.D4, 3))
        assertEquals(32, policy.buyBeeUpgradeDiePercentage(context, DieSides.D6, 3))
        assertEquals(58, policy.buyBeeUpgradeDiePercentage(context, DieSides.D8, 3))
        assertEquals(80, policy.buyBeeUpgradeDiePercentage(context, DieSides.D10, 3))

        assertEquals(20, policy.buyBeeUpgradePlantPercentage(context, 1))
        assertEquals(33, policy.buyBeeUpgradePlantPercentage(context, 2))
        assertEquals(50, policy.buyBeeUpgradePlantPercentage(context, 3))
    }

    @Test
    fun `Worm Buy bridge begins at two Worms and grows conservatively`() {
        val policy = HumanBaselinePolicy()
        val context = context()

        assertEquals(0, policy.buyWormBridgePercentage(context, 1))
        assertEquals(5, policy.buyWormBridgePercentage(context, 2))
        assertEquals(10, policy.buyWormBridgePercentage(context, 3))
        assertEquals(17, policy.buyWormBridgePercentage(context, 4))
        assertEquals(30, policy.buyWormBridgePercentage(context, 5))
    }

    @Test
    fun `Buy overpay concern grows exponentially after one`() {
        val policy = HumanBaselinePolicy()
        val context = context()

        assertEquals(0, policy.buyOverpayPenalty(context, 0))
        assertEquals(1, policy.buyOverpayPenalty(context, 1))
        assertEquals(3, policy.buyOverpayPenalty(context, 2))
        assertEquals(7, policy.buyOverpayPenalty(context, 3))
        assertEquals(15, policy.buyOverpayPenalty(context, 4))
    }

    @Test
    fun `Buy Plant tier curve and follow up reserve are configurable`() {
        val policy = HumanBaselinePolicy(
            buyPlantCostTierExponentialBaseValue = 1.5,
            buyCheaperPlantMinimumRemainingDiceValue = 7
        )

        assertTrue(
            policy.buyPlantCostTierWeight(context(), 1) >
                policy.buyPlantCostTierWeight(context(), 0)
        )
        assertEquals(7, policy.buyCheaperPlantMinimumRemainingDice(context()))
    }

    @Test
    fun `default Battle policy centralizes approved Stage A thresholds`() {
        val policy = HumanBaselinePolicy()
        val context = context().copy(phase = RoundCardType.BATTLE)

        assertEquals(100, policy.battleTransitionScale(context))
        assertEquals(4, policy.battleCloseMargin(context))
        assertEquals(10, policy.battleSecuredLead(context))
        assertEquals(10, policy.battleHopelessDeficit(context))
        assertEquals(2, policy.battleMinimumMeaningfulVpGain(context))
        assertEquals(2, policy.battleWaterRefreshMinImprovementSteps(context))
        assertEquals(5, policy.battleImmediateResolveMinLead(context))
        assertEquals(3, policy.battleImmediateResolveMinOpponentSupport(context))
    }

    @Test
    fun `constructor knobs can tune Battle policy without changing production constants`() {
        val policy = HumanBaselinePolicy(
            battleTransitionScaleValue = 200,
            battleCloseMarginValue = 3,
            battleSecuredLeadValue = 12,
            battleHopelessDeficitValue = 11,
            battleMinimumMeaningfulVpGainValue = 4,
            battleWaterRefreshMinImprovementStepsValue = 3,
            battleImmediateResolveMinLeadValue = 6,
            battleImmediateResolveMinOpponentSupportValue = 4
        )
        val context = context().copy(phase = RoundCardType.BATTLE)

        assertEquals(200, policy.battleTransitionScale(context))
        assertEquals(3, policy.battleCloseMargin(context))
        assertEquals(12, policy.battleSecuredLead(context))
        assertEquals(11, policy.battleHopelessDeficit(context))
        assertEquals(4, policy.battleMinimumMeaningfulVpGain(context))
        assertEquals(3, policy.battleWaterRefreshMinImprovementSteps(context))
        assertEquals(6, policy.battleImmediateResolveMinLead(context))
        assertEquals(4, policy.battleImmediateResolveMinOpponentSupport(context))
    }

    @Test
    fun `Battle strategy code can override policy by decision context`() {
        val policy = object : HumanBaselinePolicy() {
            override fun battleSecuredLead(context: DecisionContext): Int =
                if (context.phase == RoundCardType.BATTLE) 7 else 10
        }

        assertEquals(7, policy.battleSecuredLead(context().copy(phase = RoundCardType.BATTLE)))
        assertEquals(10, policy.battleSecuredLead(context()))
    }

    private fun context(
        hand: List<DieView> = emptyList(),
        bees: Int = 0,
        worms: Int = 0,
        plantCount: Int = 0,
        progress: GameProgressView = GameProgressView.EMPTY
    ): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        progress = progress,
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(
                hand = hand,
                bees = bees,
                worms = worms,
                creature = List(plantCount) { index -> testPlant(index) }
            )
        )
    )

    private fun testPlant(index: Int) = CreatureCardView(
        id = CreatureCardId(index + 1),
        name = "TestPlant${index + 1}",
        title = "Test Plant ${index + 1}",
        type = PlantType.VINE,
        cost = 7,
        effect = GameEffect.RAISE_ANY_DIE_PLUS_1,
        scoringRule = PlantScoringRule.Fixed(1),
        side = CreatureSide.LEFT,
        position = CreaturePosition(index, 0),
        facing = CreatureCard.Facing.FACE_UP,
        isSnippable = true
    )
}
