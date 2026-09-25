package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.common.ResourceReserveTargets
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.buy.*
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HumanBaselineBuyStrategyTest {

    @Nested
    inner class `Human Baseline Behavior Contract` {
        @Test
        fun `three point dice lead gives Plant category an eighty five percent boundary`() {
            val existing = listOf(
                view(plant("Root_05_01", PlantType.ROOT, 5, GameEffect.DOUBLE_ONE_DIE)),
                view(plant("Vine_07_01", PlantType.VINE, 7, GameEffect.RAISE_ANY_DIE_PLUS_1)),
                view(plant("Flower_11_01", PlantType.FLOWER, 11, GameEffect.GAIN_ONE_VP))
            )
            val newPlant = plant("Root_07_02", PlantType.ROOT, 7, GameEffect.RAISE_DIE_PLUS_4)
            val request = ChoosePurchaseRequest(
                options = listOf(BuyItem.Plant(newPlant), BuyItem.Die(DieSides.D12)),
                context = context(
                    plantCards = existing,
                    dice = listOf(
                        DieView(0, 8, 8),
                        DieView(1, 10, 10),
                        DieView(2, 10, 10),
                        DieView(3, 20, 20)
                    ),
                    battleRoundsCompleted = 1
                )
            )

            val acceptsPlant = strategy(QueueRandomizer(84)).choosePurchase(request)
            val takesDie = strategy(QueueRandomizer(85)).choosePurchase(request)

            assertEquals(BuyItem.Plant(newPlant), assertIs<BuyChoice.Purchase>(acceptsPlant).item)
            assertEquals(BuyItem.Die(DieSides.D12), assertIs<BuyChoice.Purchase>(takesDie).item)
        }

        @Test
        fun `three point Plant lead gives die category an eighty five percent boundary`() {
            val existing = listOf(
                view(plant("Root_05_01", PlantType.ROOT, 5, GameEffect.DOUBLE_ONE_DIE)),
                view(plant("Vine_07_01", PlantType.VINE, 7, GameEffect.RAISE_ANY_DIE_PLUS_1)),
                view(plant("Flower_11_01", PlantType.FLOWER, 11, GameEffect.GAIN_ONE_VP))
            )
            val newPlant = plant("Root_09_01", PlantType.ROOT, 9, GameEffect.UPGRADE_DIE_AND_USE_NOW)
            val request = ChoosePurchaseRequest(
                options = listOf(BuyItem.Plant(newPlant), BuyItem.Die(DieSides.D8)),
                context = context(
                    plantCards = existing,
                    dice = listOf(
                        DieView(0, 20, 20),
                        DieView(1, 12, 12),
                        DieView(2, 10, 10)
                    ),
                    battleRoundsCompleted = 1
                )
            )

            val rarePlant = strategy(QueueRandomizer(14)).choosePurchase(request)
            val expectedDie = strategy(QueueRandomizer(15)).choosePurchase(request)

            assertEquals(BuyItem.Plant(newPlant), assertIs<BuyChoice.Purchase>(rarePlant).item)
            assertEquals(BuyItem.Die(DieSides.D8), assertIs<BuyChoice.Purchase>(expectedDie).item)
        }

        @Test
        fun `two Plants against forty eight dice power strongly buys another Plant`() {
            val existing = listOf(
                view(plant("Root_05_01", PlantType.ROOT, 5, GameEffect.DOUBLE_ONE_DIE)),
                view(plant("Vine_07_01", PlantType.VINE, 7, GameEffect.RAISE_ANY_DIE_PLUS_1))
            )
            val newPlant = plant("Flower_11_01", PlantType.FLOWER, 11, GameEffect.GAIN_ONE_VP)
            val request = ChoosePurchaseRequest(
                options = listOf(BuyItem.Plant(newPlant), BuyItem.Die(DieSides.D20)),
                context = context(
                    plantCards = existing,
                    dice = listOf(
                        DieView(0, 8, 8),
                        DieView(1, 10, 10),
                        DieView(2, 10, 10),
                        DieView(3, 20, 20)
                    ),
                    battleRoundsCompleted = 1
                )
            )

            val expectedPlant = strategy(QueueRandomizer(98)).choosePurchase(request)
            val rareDie = strategy(QueueRandomizer(99)).choosePurchase(request)

            assertEquals(BuyItem.Plant(newPlant), assertIs<BuyChoice.Purchase>(expectedPlant).item)
            assertEquals(BuyItem.Die(DieSides.D20), assertIs<BuyChoice.Purchase>(rareDie).item)
        }

        @Test
        fun `equal Plant and dice power is fifty fifty independent of cultivation round`() {
            val existing = listOf(
                view(plant("Root_05_01", PlantType.ROOT, 5, GameEffect.DOUBLE_ONE_DIE)),
                view(plant("Vine_07_01", PlantType.VINE, 7, GameEffect.RAISE_ANY_DIE_PLUS_1))
            )
            val newPlant = plant("Root_07_02", PlantType.ROOT, 7, GameEffect.RAISE_DIE_PLUS_4)
            val options = listOf(BuyItem.Plant(newPlant), BuyItem.Die(DieSides.D12))
            val dice = listOf(
                DieView(0, 6, 6),
                DieView(1, 6, 6),
                DieView(2, 6, 6),
                DieView(3, 6, 6),
                DieView(4, 6, 6)
            )

            val roundFive = ChoosePurchaseRequest(
                options = options,
                context = context(
                    plantCards = existing,
                    dice = dice,
                    cultivationRound = 5,
                    battleRoundsCompleted = 1
                )
            )
            val roundEight = ChoosePurchaseRequest(
                options = options,
                context = roundFive.context.copy(
                    progress = roundFive.context.progress.copy(currentCultivationRoundNumber = 8)
                )
            )

            assertEquals(
                BuyItem.Plant(newPlant),
                assertIs<BuyChoice.Purchase>(strategy(QueueRandomizer(49)).choosePurchase(roundFive)).item
            )
            assertEquals(
                BuyItem.Die(DieSides.D12),
                assertIs<BuyChoice.Purchase>(strategy(QueueRandomizer(50)).choosePurchase(roundFive)).item
            )
            assertEquals(
                BuyItem.Plant(newPlant),
                assertIs<BuyChoice.Purchase>(strategy(QueueRandomizer(49)).choosePurchase(roundEight)).item
            )
            assertEquals(
                BuyItem.Die(DieSides.D12),
                assertIs<BuyChoice.Purchase>(strategy(QueueRandomizer(50)).choosePurchase(roundEight)).item
            )
        }

        @Test
        fun `Buy category balance can be overridden by player policy`() {
            val newPlant = plant("Root_07_02", PlantType.ROOT, 7, GameEffect.RAISE_DIE_PLUS_4)
            val policy = object : HumanBaselinePolicy() {
                override fun buyPlantPriorityPercentage(context: DecisionContext): Int = 0
            }
            val request = ChoosePurchaseRequest(
                options = listOf(BuyItem.Plant(newPlant), BuyItem.Die(DieSides.D12)),
                context = context(
                    plantCards = emptyList(),
                    dice = listOf(DieView(0, 20, 20), DieView(1, 20, 20)),
                    battleRoundsCompleted = 1
                )
            )

            val chosen = strategy(QueueRandomizer(0), policy).choosePurchase(request)

            assertEquals(BuyItem.Die(DieSides.D12), assertIs<BuyChoice.Purchase>(chosen).item)
        }

        @Test
        fun `before first Battle ninety percent branch protects a two Plant floor`() {
            val existing = view(plant("Root_05_01", PlantType.ROOT, 5, GameEffect.DOUBLE_ONE_DIE))
            val newPlant = plant("Vine_09_01", PlantType.VINE, 9, GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX)
            val request = ChoosePurchaseRequest(
                options = listOf(BuyItem.Plant(newPlant), BuyItem.Die(DieSides.D12)),
                context = context(
                    plantCards = listOf(existing),
                    dice = listOf(
                        DieView(0, 10, 4),
                        DieView(1, 10, 4),
                        DieView(2, 10, 4)
                    ),
                    cultivationRound = 2,
                    battleRoundsCompleted = 0
                )
            )

            val acceptsPlantPriority = strategy(QueueRandomizer(89)).choosePurchase(request)
            val declinesPlantPriority = strategy(QueueRandomizer(90)).choosePurchase(request)

            assertEquals(
                BuyItem.Plant(newPlant),
                assertIs<BuyChoice.Purchase>(acceptsPlantPriority).item
            )
            assertEquals(
                BuyItem.Die(DieSides.D12),
                assertIs<BuyChoice.Purchase>(declinesPlantPriority).item
            )
        }

        @Test
        fun `within Plant category choose highest affordable cost tier`() {
            val cheap = plant("Root_07_01", PlantType.ROOT, 7, GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE)
            val expensive = plant("Root_09_01", PlantType.ROOT, 9, GameEffect.UPGRADE_DIE_AND_USE_NOW)
            val context = context(
                plantCards = emptyList(),
                dice = listOf(DieView(0, 20, 10), DieView(1, 20, 10))
            )

            val chosen = strategy().choosePurchase(
                ChoosePurchaseRequest(
                    options = listOf(BuyItem.Plant(cheap), BuyItem.Plant(expensive)),
                    context = context
                )
            )

            assertEquals(BuyItem.Plant(expensive), assertIs<BuyChoice.Purchase>(chosen).item)
        }

        @Test
        fun `after one principal purchase Human Baseline stops buying`() {
            val chosen = strategy().choosePurchase(
                ChoosePurchaseRequest(
                    options = listOf(BuyItem.Die(DieSides.D20)),
                    context = context(dice = listOf(DieView(0, 20, 20))),
                    purchasesMadeThisBuy = 1
                )
            )

            assertEquals(BuyChoice.Done, chosen)
        }

        @Test
        fun `zero Critter surplus is not normally available for a non-premium purchase`() {
            val root = plant("Root_07_01", PlantType.ROOT, 7, GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE)
            val chosen = strategy().choosePurchase(
                ChoosePurchaseRequest(
                    options = listOf(BuyItem.Plant(root)),
                    context = context(
                        dice = listOf(DieView(0, 6, 6)),
                        bees = 2,
                        worms = 1
                    )
                )
            )

            assertEquals(BuyChoice.Done, chosen)
        }

        @Test
        fun `one surplus Critter uses 20 percent threshold`() {
            val root = plant("Root_07_01", PlantType.ROOT, 7, GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE)
            val request = ChoosePurchaseRequest(
                options = listOf(BuyItem.Plant(root)),
                context = context(
                    dice = listOf(DieView(0, 6, 6)),
                    bees = 3,
                    worms = 1
                )
            )

            assertIs<BuyChoice.Purchase>(strategy(QueueRandomizer(19)).choosePurchase(request))
            assertEquals(BuyChoice.Done, strategy(QueueRandomizer(20)).choosePurchase(request))
        }

        @Test
        fun `Critter surplus probability follows configured formula`() {
            val strategy = strategy()
            assertEquals(0, strategy.critterSpendPercentage(0))
            assertEquals(20, strategy.critterSpendPercentage(1))
            assertEquals(35, strategy.critterSpendPercentage(2))
            assertEquals(50, strategy.critterSpendPercentage(3))
            assertEquals(65, strategy.critterSpendPercentage(4))
            assertEquals(80, strategy.critterSpendPercentage(5))
            assertEquals(95, strategy.critterSpendPercentage(6))
            assertEquals(100, strategy.critterSpendPercentage(7))
            assertEquals(100, strategy.critterSpendPercentage(20))
        }


        @Test
        fun `Buy uses the injected Human Baseline Critter reserve policy`() {
            val root = plant("Root_07_01", PlantType.ROOT, 7, GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE)
            val policy = object : HumanBaselinePolicy() {
                override fun protectedCritterReserve(context: DecisionContext) =
                    ResourceReserveTargets(bees = 1, worms = 1)
            }
            val request = ChoosePurchaseRequest(
                options = listOf(BuyItem.Plant(root)),
                context = context(
                    dice = listOf(DieView(0, 6, 6)),
                    bees = 2,
                    worms = 1
                )
            )

            assertIs<BuyChoice.Purchase>(strategy(QueueRandomizer(0), policy).choosePurchase(request))
        }

        @Test
        fun `D20 threshold may spend protected Bee reserve`() {
            val chosen = strategy().choosePurchase(
                ChoosePurchaseRequest(
                    options = listOf(BuyItem.Die(DieSides.D20)),
                    context = context(
                        dice = listOf(DieView(0, 20, 18)),
                        bees = 2,
                        worms = 1
                    )
                )
            )

            assertEquals(BuyItem.Die(DieSides.D20), assertIs<BuyChoice.Purchase>(chosen).item)
        }

        @Test
        fun `cost 17 Flower threshold may spend protected Critter reserve`() {
            val flower = plant("Flower_17_01", PlantType.FLOWER, 17, GameEffect.DRAW_TWO_DICE)
            val chosen = strategy().choosePurchase(
                ChoosePurchaseRequest(
                    options = listOf(BuyItem.Plant(flower)),
                    context = context(
                        dice = listOf(DieView(0, 20, 15)),
                        bees = 2,
                        worms = 1
                    )
                )
            )

            assertEquals(BuyItem.Plant(flower), assertIs<BuyChoice.Purchase>(chosen).item)
        }

        @Test
        fun `when a Critter is required Bee is preferred on the 67 percent branch`() {
            val random = QueueRandomizer(0, 0) // surplus allowed; then Bee preference
            val chosen = strategy(random).choosePayment(
                paymentRequest(
                    context = context(bees = 3, worms = 2),
                    cost = 7
                )
            )

            assertTrue(chosen.critters.any { it.critter == Critter.BEE })
        }

        @Test
        fun `when a Critter is required Worm is preferred on the other branch`() {
            val random = QueueRandomizer(0, 99) // surplus allowed; then Worm preference
            val chosen = strategy(random).choosePayment(
                paymentRequest(
                    context = context(bees = 3, worms = 2),
                    cost = 7
                )
            )

            assertTrue(chosen.critters.any { it.critter == Critter.WORM })
        }
    }

    @Nested
    inner class `Implementation and edge cases` {
        @Test
        fun `no offered purchases returns Done`() {
            assertEquals(
                BuyChoice.Done,
                strategy().choosePurchase(
                    ChoosePurchaseRequest(
                        options = emptyList(),
                        context = context()
                    )
                )
            )
        }

        @Test
        fun `complete payment candidates prefer exact sufficient payment`() {
            val chosen = strategy().choosePayment(
                ChoosePaymentRequest(
                    item = BuyItem.Die(DieSides.D6),
                    availableDice = listOf(
                        BuyDieResource(sides = 4, value = 4),
                        BuyDieResource(sides = 6, value = 6)
                    ),
                    availableCritters = emptyList(),
                    context = context()
                )
            )

            assertEquals(listOf(BuyDieResource(6, 6)), chosen.dice)
        }
    }

    private fun strategy(
        randomizer: StrategyRandomizer = QueueRandomizer(0),
        policy: HumanBaselinePolicy = HumanBaselinePolicy()
    ) = HumanBaselineBuyStrategy(
        scoreEngine = BaselineScoreEngine(randomizer = randomizer),
        strategyRandomizer = randomizer,
        policy = policy
    )

    private fun paymentRequest(context: DecisionContext, cost: Int): ChoosePaymentRequest =
        ChoosePaymentRequest(
            item = BuyItem.Plant(
                plant("Root_07_01", PlantType.ROOT, cost, GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE)
            ),
            availableDice = listOf(BuyDieResource(6, 6)),
            availableCritters = listOf(
                BuyCritterResource(Critter.BEE, 2),
                BuyCritterResource(Critter.WORM, 1)
            ),
            context = context
        )

    private fun context(
        plantCards: List<CreatureCardView> = listOf(view(plant("Root_05_01", PlantType.ROOT, 5, GameEffect.DOUBLE_ONE_DIE))),
        dice: List<DieView> = listOf(DieView(0, 20, 10), DieView(1, 20, 10)),
        bees: Int = 0,
        worms: Int = 0,
        cultivationRound: Int = 1,
        battleRoundsCompleted: Int = 0
    ): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        progress = DecisionContext.EMPTY.progress.copy(
            currentCultivationRoundNumber = cultivationRound,
            totalCultivationRounds = 8,
            battleRoundsCompleted = battleRoundsCompleted
        ),
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(
                id = PlayerId(1),
                hand = dice,
                bees = bees,
                worms = worms,
                creature = plantCards
            )
        )
    )

    private fun plant(name: String, type: PlantType, cost: Int, effect: GameEffect) = PlantCard(
        quantity = 6,
        name = name,
        title = name,
        type = type,
        cost = cost,
        lineIcon = null,
        vpIcon = "",
        typeIcon = "",
        fgColor = "",
        textColor = "",
        fullImage = "",
        backgroundImage = "",
        cardBackgroundImage = "",
        effect = effect,
        scoringRule = PlantScoringRule.Fixed(1)
    )

    private fun view(card: PlantCard) = CreatureCardView(
        id = CreatureCardId(1),
        name = card.name,
        title = card.title,
        type = card.type,
        cost = card.cost,
        effect = card.effect,
        scoringRule = card.scoringRule,
        side = CreatureSide.LEFT,
        position = CreaturePosition(-1, 0),
        facing = CreatureCard.Facing.FACE_UP,
        isSnippable = true
    )

    private class QueueRandomizer(vararg values: Int) : StrategyRandomizer {
        private val values = ArrayDeque(values.toList())
        override fun nextInt(until: Int): Int {
            val value = if (values.isEmpty()) 0 else values.removeFirst()
            require(value in 0 until until) { "Scripted strategy random value $value is outside 0 until $until" }
            return value
        }
    }
}
