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
        fun `Buy category balance can be overridden by player policy once low Plant safety is satisfied`() {
            val existing = listOf(
                view(plant("Root_05_01", PlantType.ROOT, 5, GameEffect.DOUBLE_ONE_DIE)),
                view(plant("Vine_07_01", PlantType.VINE, 7, GameEffect.RAISE_ANY_DIE_PLUS_1))
            )
            val newPlant = plant("Root_07_02", PlantType.ROOT, 7, GameEffect.RAISE_DIE_PLUS_4)
            val policy = object : HumanBaselinePolicy() {
                override fun buyPlantPriorityPercentage(context: DecisionContext): Int = 0
            }
            val request = ChoosePurchaseRequest(
                options = listOf(BuyItem.Plant(newPlant), BuyItem.Die(DieSides.D12)),
                context = context(
                    plantCards = existing,
                    dice = listOf(DieView(0, 20, 20), DieView(1, 20, 20)),
                    battleRoundsCompleted = 1
                )
            )

            val chosen = strategy(QueueRandomizer(0), policy).choosePurchase(request)

            assertEquals(BuyItem.Die(DieSides.D12), assertIs<BuyChoice.Purchase>(chosen).item)
        }

        @Test
        fun `ninety percent low Plant branch applies even after prior Battles`() {
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
                    cultivationRound = 6,
                    battleRoundsCompleted = 2
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
        fun `normal Plant versus dice balance resumes at two Plants`() {
            val existing = listOf(
                view(plant("Root_05_01", PlantType.ROOT, 5, GameEffect.DOUBLE_ONE_DIE)),
                view(plant("Vine_07_01", PlantType.VINE, 7, GameEffect.RAISE_ANY_DIE_PLUS_1))
            )
            val newPlant = plant("Vine_09_01", PlantType.VINE, 9, GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX)
            val request = ChoosePurchaseRequest(
                options = listOf(BuyItem.Plant(newPlant), BuyItem.Die(DieSides.D12)),
                context = context(
                    plantCards = existing,
                    dice = listOf(
                        DieView(0, 6, 6),
                        DieView(1, 6, 6),
                        DieView(2, 6, 6),
                        DieView(3, 6, 6),
                        DieView(4, 6, 6)
                    ),
                    cultivationRound = 6,
                    battleRoundsCompleted = 2
                )
            )

            val plantAt49 = strategy(QueueRandomizer(49)).choosePurchase(request)
            val dieAt50 = strategy(QueueRandomizer(50)).choosePurchase(request)

            assertEquals(BuyItem.Plant(newPlant), assertIs<BuyChoice.Purchase>(plantAt49).item)
            assertEquals(BuyItem.Die(DieSides.D12), assertIs<BuyChoice.Purchase>(dieAt50).item)
        }

        @Test
        fun `Plant cost tiers use exponential weighting with about two percent on R5`() {
            val policy = HumanBaselinePolicy()
            val weights = (0..5).map { policy.buyPlantCostTierWeight(DecisionContext.EMPTY, it) }
            val cheapestPercentage = weights.first().toDouble() / weights.sum() * 100.0

            assertTrue(cheapestPercentage in 1.9..2.1)
            assertTrue(weights.zipWithNext().all { (a, b) -> b > a })
        }

        @Test
        fun `Plant weighting can select cheap tier while expensive tier remains most likely`() {
            val plants = listOf(
                plant("Root_05_01", PlantType.ROOT, 5, GameEffect.DOUBLE_ONE_DIE),
                plant("Root_07_01", PlantType.ROOT, 7, GameEffect.RAISE_ANY_DIE_PLUS_1),
                plant("Root_09_01", PlantType.ROOT, 9, GameEffect.UPGRADE_DIE_AND_USE_NOW),
                plant("Vine_11_01", PlantType.VINE, 11, GameEffect.SET_DIE_UP_TO_D12_TO_MAX),
                plant("Flower_14_01", PlantType.FLOWER, 14, GameEffect.GAIN_ONE_VP),
                plant("Flower_17_01", PlantType.FLOWER, 17, GameEffect.DRAW_TWO_DICE)
            )
            val context = context(
                plantCards = listOf(
                    view(plant("Vine_07_existing", PlantType.VINE, 7, GameEffect.RAISE_ANY_DIE_PLUS_1))
                ),
                dice = listOf(DieView(0, 20, 20), DieView(1, 20, 20))
            )
            val request = ChoosePurchaseRequest(plants.map(BuyItem::Plant), context)
            val rareCheap = strategy(QueueRandomizer(0)).choosePurchase(request)
            val commonExpensive = strategy(QueueRandomizer(999_999)).choosePurchase(request)

            assertEquals(5, assertIs<BuyChoice.Purchase>(rareCheap).item.cost)
            assertEquals(17, assertIs<BuyChoice.Purchase>(commonExpensive).item.cost)
        }

        @Test
        fun `recursive planner uses the exact chosen dice group as payment`() {
            val root5 = plant("Root_05_01", PlantType.ROOT, 5, GameEffect.DOUBLE_ONE_DIE)
            val context = context(
                plantCards = emptyList(),
                dice = listOf(DieView(0, 6, 5), DieView(1, 6, 5), DieView(2, 6, 5))
            )
            val options = listOf(BuyItem.Plant(root5), BuyItem.Die(DieSides.D10))
            val strategy = strategy(MidpointRandomizer())
            val choice = strategy.choosePurchase(
                ChoosePurchaseRequest(options, context, marketOptions = options)
            )
            assertEquals(BuyItem.Plant(root5), assertIs<BuyChoice.Purchase>(choice).item)

            val payment = strategy.choosePayment(
                ChoosePaymentRequest(
                    item = BuyItem.Plant(root5),
                    availableDice = context.self.board.hand.map { BuyDieResource(it.sides, it.value) },
                    availableCritters = emptyList(),
                    context = context
                )
            )

            assertEquals(5, payment.total)
            assertEquals(1, payment.dice.size)
        }

        @Test
        fun `recursive Buy planning prefers R5 plus D10 over V11 with four overpay`() {
            val root5 = plant("Root_05_02", PlantType.ROOT, 5, GameEffect.RAISE_DIE_PLUS_4)
            val vine9 = plant("Vine_09_01", PlantType.VINE, 9, GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX)
            val vine11 = plant("Vine_11_04", PlantType.VINE, 11, GameEffect.SET_DIE_UP_TO_D12_TO_MAX)
            val existingRoot = view(plant("Root_05_01", PlantType.ROOT, 5, GameEffect.DOUBLE_ONE_DIE))
            val firstContext = context(
                plantCards = listOf(existingRoot),
                dice = listOf(
                    DieView(0, 6, 5),
                    DieView(1, 6, 5),
                    DieView(2, 6, 5)
                )
            )
            val market = listOf(
                BuyItem.Plant(root5),
                BuyItem.Plant(vine9),
                BuyItem.Plant(vine11),
                BuyItem.Die(DieSides.D4),
                BuyItem.Die(DieSides.D10)
            )
            val strategy = strategy(MidpointRandomizer())

            val first = strategy.choosePurchase(
                ChoosePurchaseRequest(
                    options = market,
                    context = firstContext,
                    marketOptions = market
                )
            )
            assertEquals(BuyItem.Plant(root5), assertIs<BuyChoice.Purchase>(first).item)
            val firstPayment = strategy.choosePayment(
                ChoosePaymentRequest(
                    item = BuyItem.Plant(root5),
                    availableDice = firstContext.self.board.hand.map { BuyDieResource(it.sides, it.value) },
                    availableCritters = emptyList(),
                    context = firstContext
                )
            )
            assertEquals(5, firstPayment.total)
            assertEquals(1, firstPayment.dice.size)

            val secondContext = firstContext.copy(
                self = firstContext.self.copy(
                    board = firstContext.self.board.copy(
                        hand = listOf(DieView(1, 6, 5), DieView(2, 6, 5)),
                        creature = firstContext.self.board.creature + view(root5)
                    )
                )
            )
            val second = strategy.choosePurchase(
                ChoosePurchaseRequest(
                    options = listOf(BuyItem.Die(DieSides.D4), BuyItem.Die(DieSides.D10)),
                    context = secondContext,
                    purchasesMadeThisBuy = 1,
                    marketOptions = market
                )
            )
            assertEquals(BuyItem.Die(DieSides.D10), assertIs<BuyChoice.Purchase>(second).item)
            val secondPayment = strategy.choosePayment(
                ChoosePaymentRequest(
                    item = BuyItem.Die(DieSides.D10),
                    availableDice = secondContext.self.board.hand.map { BuyDieResource(it.sides, it.value) },
                    availableCritters = emptyList(),
                    context = secondContext
                )
            )
            assertEquals(10, secondPayment.total)
            assertEquals(2, secondPayment.dice.size)
        }

        @Test
        fun `planner can project a Flower becoming legal after buying a Vine`() {
            val vine11 = plant("Vine_11_04", PlantType.VINE, 11, GameEffect.SET_DIE_UP_TO_D12_TO_MAX)
            val flower17 = plant("Flower_17_04", PlantType.FLOWER, 17, GameEffect.DRAW_TWO_DICE)
            val firstContext = context(
                plantCards = emptyList(),
                dice = listOf(DieView(0, 12, 11), DieView(1, 20, 17))
            )
            val strategy = strategy(MidpointRandomizer())
            val market = listOf(BuyItem.Plant(vine11), BuyItem.Plant(flower17))

            val first = strategy.choosePurchase(
                ChoosePurchaseRequest(
                    options = listOf(BuyItem.Plant(vine11)),
                    context = firstContext,
                    marketOptions = market
                )
            )
            assertEquals(BuyItem.Plant(vine11), assertIs<BuyChoice.Purchase>(first).item)
            strategy.choosePayment(
                ChoosePaymentRequest(
                    item = BuyItem.Plant(vine11),
                    availableDice = firstContext.self.board.hand.map { BuyDieResource(it.sides, it.value) },
                    availableCritters = emptyList(),
                    context = firstContext
                )
            )

            val vineView = view(vine11)
            val secondContext = firstContext.copy(
                self = firstContext.self.copy(
                    board = firstContext.self.board.copy(
                        hand = listOf(DieView(1, 20, 17)),
                        creature = listOf(vineView)
                    )
                )
            )
            val second = strategy.choosePurchase(
                ChoosePurchaseRequest(
                    options = listOf(BuyItem.Plant(flower17)),
                    context = secondContext,
                    purchasesMadeThisBuy = 1,
                    marketOptions = listOf(BuyItem.Plant(flower17))
                )
            )

            assertEquals(BuyItem.Plant(flower17), assertIs<BuyChoice.Purchase>(second).item)
        }

        @Test
        fun `one Bee makes D4 to D6 a four percent Buy bridge`() {
            val context = context(
                dice = listOf(DieView(0, 4, 4)),
                bees = 1
            )
            val options = listOf(BuyItem.Die(DieSides.D4), BuyItem.Die(DieSides.D6))
            val request = ChoosePurchaseRequest(options, context, marketOptions = options)

            val upgradingStrategy = strategy(QueueRandomizer(3))
            val upgrade = upgradingStrategy.choosePurchase(request)
            val stay = strategy(QueueRandomizer(4)).choosePurchase(request)

            assertEquals(BuyItem.Die(DieSides.D6), assertIs<BuyChoice.Purchase>(upgrade).item)
            val upgradePayment = upgradingStrategy.choosePayment(
                ChoosePaymentRequest(
                    item = BuyItem.Die(DieSides.D6),
                    availableDice = listOf(BuyDieResource(4, 4)),
                    availableCritters = listOf(BuyCritterResource(Critter.BEE, 2)),
                    context = context
                )
            )
            assertEquals(6, upgradePayment.total)
            assertEquals(1, upgradePayment.critters.count { it.critter == Critter.BEE })
            assertEquals(BuyItem.Die(DieSides.D4), assertIs<BuyChoice.Purchase>(stay).item)
        }

        @Test
        fun `three Bees make D10 to D12 an eighty percent Buy bridge`() {
            val context = context(
                dice = listOf(DieView(0, 10, 10)),
                bees = 3
            )
            val options = listOf(BuyItem.Die(DieSides.D10), BuyItem.Die(DieSides.D12))
            val request = ChoosePurchaseRequest(options, context, marketOptions = options)

            assertEquals(
                BuyItem.Die(DieSides.D12),
                assertIs<BuyChoice.Purchase>(strategy(QueueRandomizer(79)).choosePurchase(request)).item
            )
            assertEquals(
                BuyItem.Die(DieSides.D10),
                assertIs<BuyChoice.Purchase>(strategy(QueueRandomizer(80)).choosePurchase(request)).item
            )
        }

        @Test
        fun `three Bees make R5 to cost seven Plant upgrade fifty percent`() {
            val root5 = plant("Root_05_01", PlantType.ROOT, 5, GameEffect.DOUBLE_ONE_DIE)
            val root7 = plant("Root_07_01", PlantType.ROOT, 7, GameEffect.RAISE_ANY_DIE_PLUS_1)
            val context = context(
                plantCards = emptyList(),
                dice = listOf(DieView(0, 6, 5)),
                bees = 3
            )
            val options = listOf(BuyItem.Plant(root5), BuyItem.Plant(root7))
            val request = ChoosePurchaseRequest(options, context, marketOptions = options)

            val upgrade = strategy(QueueRandomizer(49)).choosePurchase(request)
            val stay = strategy(QueueRandomizer(50)).choosePurchase(request)

            assertEquals(BuyItem.Plant(root7), assertIs<BuyChoice.Purchase>(upgrade).item)
            assertEquals(BuyItem.Plant(root5), assertIs<BuyChoice.Purchase>(stay).item)
        }

        @Test
        fun `Bee Plant bridge can reach fourteen and seventeen cost tiers`() {
            val vine = view(plant("Vine_07_existing", PlantType.VINE, 7, GameEffect.RAISE_ANY_DIE_PLUS_1))

            val flower14 = plant("Flower_14_01", PlantType.FLOWER, 14, GameEffect.GAIN_ONE_VP)
            val vine11 = plant("Vine_11_01", PlantType.VINE, 11, GameEffect.SET_DIE_UP_TO_D12_TO_MAX)
            val context13 = context(
                plantCards = listOf(vine),
                dice = listOf(DieView(0, 20, 13)),
                bees = 1
            )
            val options14 = listOf(BuyItem.Plant(vine11), BuyItem.Plant(flower14))
            assertEquals(
                BuyItem.Plant(flower14),
                assertIs<BuyChoice.Purchase>(
                    strategy(QueueRandomizer(0)).choosePurchase(
                        ChoosePurchaseRequest(options14, context13, marketOptions = options14)
                    )
                ).item
            )

            val flower17 = plant("Flower_17_01", PlantType.FLOWER, 17, GameEffect.DRAW_TWO_DICE)
            val context16 = context(
                plantCards = listOf(vine),
                dice = listOf(DieView(0, 20, 16)),
                bees = 1
            )
            val options17 = listOf(BuyItem.Plant(flower14), BuyItem.Plant(flower17))
            assertEquals(
                BuyItem.Plant(flower17),
                assertIs<BuyChoice.Purchase>(
                    strategy(QueueRandomizer(0)).choosePurchase(
                        ChoosePurchaseRequest(options17, context16, marketOptions = options17)
                    )
                ).item
            )
        }

        @Test
        fun `two Worms can bridge a purchase only when exactly one point short`() {
            val exactGapContext = context(
                dice = listOf(DieView(0, 6, 5)),
                worms = 2
            )
            val dieOptions = listOf(BuyItem.Die(DieSides.D4), BuyItem.Die(DieSides.D6))
            val upgradeStrategy = strategy(QueueRandomizer(4))
            val upgraded = upgradeStrategy.choosePurchase(
                ChoosePurchaseRequest(dieOptions, exactGapContext, marketOptions = dieOptions)
            )
            assertEquals(BuyItem.Die(DieSides.D6), assertIs<BuyChoice.Purchase>(upgraded).item)
            val payment = upgradeStrategy.choosePayment(
                ChoosePaymentRequest(
                    item = BuyItem.Die(DieSides.D6),
                    availableDice = listOf(BuyDieResource(6, 5)),
                    availableCritters = List(2) { BuyCritterResource(Critter.WORM, 1) },
                    context = exactGapContext
                )
            )
            assertEquals(1, payment.critters.count { it.critter == Critter.WORM })

            assertEquals(
                BuyItem.Die(DieSides.D4),
                assertIs<BuyChoice.Purchase>(
                    strategy(QueueRandomizer(5)).choosePurchase(
                        ChoosePurchaseRequest(dieOptions, exactGapContext, marketOptions = dieOptions)
                    )
                ).item
            )

            val twoShortContext = context(
                dice = listOf(DieView(0, 6, 4)),
                worms = 5
            )
            assertEquals(
                BuyItem.Die(DieSides.D4),
                assertIs<BuyChoice.Purchase>(
                    strategy(QueueRandomizer(0)).choosePurchase(
                        ChoosePurchaseRequest(dieOptions, twoShortContext, marketOptions = dieOptions)
                    )
                ).item
            )
        }

        @Test
        fun `one shared Bee willingness roll is reused across all candidate groupings`() {
            val randomizer = RecordingRandomizer(37)
            val context = context(
                dice = listOf(
                    DieView(0, 4, 4),
                    DieView(1, 10, 10)
                ),
                bees = 1
            )
            val options = listOf(
                BuyItem.Die(DieSides.D4),
                BuyItem.Die(DieSides.D6),
                BuyItem.Die(DieSides.D10),
                BuyItem.Die(DieSides.D12)
            )

            strategy(randomizer).choosePurchase(
                ChoosePurchaseRequest(options, context, marketOptions = options)
            )

            assertEquals(1, randomizer.calls.count { it == 100 })
        }

        @Test
        fun `planner look ahead never returns more than two purchases`() {
            val root5 = plant("Root_05_02", PlantType.ROOT, 5, GameEffect.RAISE_DIE_PLUS_4)
            val context = context(
                plantCards = emptyList(),
                dice = listOf(
                    DieView(0, 6, 5),
                    DieView(1, 6, 5),
                    DieView(2, 6, 5),
                    DieView(3, 6, 5)
                )
            )
            val market = listOf(
                BuyItem.Plant(root5),
                BuyItem.Die(DieSides.D4),
                BuyItem.Die(DieSides.D10)
            )
            val planner = HumanBaselineBuyPlanner(
                policy = HumanBaselinePolicy(),
                cardScorers = dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry(),
                purchaseScoreModifier = PurchaseScoreModifier.NONE,
                strategyRandomizer = MidpointRandomizer()
            )

            val planned = planner.plan(
                ChoosePurchaseRequest(market, context, marketOptions = market)
            )

            assertTrue(planned.size <= 2)
        }

        @Test
        fun `Human Baseline may continue buying after an earlier purchase`() {
            val chosen = strategy().choosePurchase(
                ChoosePurchaseRequest(
                    options = listOf(BuyItem.Die(DieSides.D6)),
                    context = context(dice = listOf(DieView(0, 20, 6))),
                    purchasesMadeThisBuy = 1
                )
            )

            assertEquals(BuyItem.Die(DieSides.D6), assertIs<BuyChoice.Purchase>(chosen).item)
        }

        @Test
        fun `zero Critter surplus is not normally available for a purchase`() {
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
        fun `one surplus Critter uses 20 percent fallback threshold`() {
            // D12 -> D20 is intentionally not a Bee tier-step, so this isolates
            // the legacy surplus-Critter fallback rather than the new planner bridge.
            val request = ChoosePurchaseRequest(
                options = listOf(BuyItem.Die(DieSides.D20)),
                context = context(
                    dice = listOf(DieView(0, 20, 18)),
                    bees = 3,
                    worms = 1
                )
            )

            // The planner samples the shared Bee willingness roll first even
            // though D12 -> D20 is not bridgeable; the second roll is the
            // legacy surplus-Critter fallback gate being tested here.
            assertIs<BuyChoice.Purchase>(strategy(QueueRandomizer(99, 19)).choosePurchase(request))
            assertEquals(BuyChoice.Done, strategy(QueueRandomizer(99, 20)).choosePurchase(request))
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
        fun `D20 no longer spends protected Bee reserve just to cross premium threshold`() {
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

            assertEquals(BuyChoice.Done, chosen)
        }

        @Test
        fun `cost 17 Flower no longer spends protected Critter reserve just to cross premium threshold`() {
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

            assertEquals(BuyChoice.Done, chosen)
        }

        @Test
        fun `minimum overpay wins even when exact payment spends a surplus Critter`() {
            val request = ChoosePaymentRequest(
                item = BuyItem.Plant(
                    plant("Root_07_01", PlantType.ROOT, 7, GameEffect.RAISE_ANY_DIE_PLUS_1)
                ),
                availableDice = listOf(
                    BuyDieResource(6, 5),
                    BuyDieResource(8, 8)
                ),
                availableCritters = listOf(
                    BuyCritterResource(Critter.BEE, 2),
                    BuyCritterResource(Critter.BEE, 2),
                    BuyCritterResource(Critter.BEE, 2),
                    BuyCritterResource(Critter.WORM, 1)
                ),
                context = context(
                    dice = listOf(DieView(0, 6, 5), DieView(1, 8, 8)),
                    bees = 3,
                    worms = 1
                )
            )

            val chosen = strategy(QueueRandomizer(0)).choosePayment(request)

            assertEquals(7, chosen.total)
            assertEquals(listOf(BuyDieResource(6, 5)), chosen.dice)
            assertEquals(listOf(BuyCritterResource(Critter.BEE, 2)), chosen.critters)
        }

        @Test
        fun `equal overpay Critter choices prefer Bee on the 67 percent branch`() {
            val random = QueueRandomizer(0, 0) // surplus allowed; then Bee preference
            val chosen = strategy(random).choosePayment(
                equalOverpayCritterPaymentRequest(
                    context = context(bees = 3, worms = 3)
                )
            )

            assertTrue(chosen.critters.any { it.critter == Critter.BEE })
        }

        @Test
        fun `equal overpay Critter choices prefer Worm on the other branch`() {
            val random = QueueRandomizer(0, 99) // surplus allowed; then Worm preference
            val chosen = strategy(random).choosePayment(
                equalOverpayCritterPaymentRequest(
                    context = context(bees = 3, worms = 3)
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

    private fun equalOverpayCritterPaymentRequest(context: DecisionContext): ChoosePaymentRequest =
        ChoosePaymentRequest(
            item = BuyItem.Plant(
                plant("Root_08_01", PlantType.ROOT, 8, GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE)
            ),
            availableDice = listOf(BuyDieResource(6, 6)),
            availableCritters = listOf(
                BuyCritterResource(Critter.BEE, 2),
                BuyCritterResource(Critter.WORM, 1),
                BuyCritterResource(Critter.WORM, 1)
            ),
            context = context
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

    private class MidpointRandomizer : StrategyRandomizer {
        override fun nextInt(until: Int): Int = until / 2
    }

    private class RecordingRandomizer(
        private val value: Int
    ) : StrategyRandomizer {
        val calls = mutableListOf<Int>()
        override fun nextInt(until: Int): Int {
            calls += until
            return value.coerceIn(0, until - 1)
        }
    }

    private class QueueRandomizer(vararg values: Int) : StrategyRandomizer {
        private val values = ArrayDeque(values.toList())
        override fun nextInt(until: Int): Int {
            val value = if (values.isEmpty()) 0 else values.removeFirst()
            require(value in 0 until until) { "Scripted strategy random value $value is outside 0 until $until" }
            return value
        }
    }
}
