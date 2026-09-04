package dugsolutions.leaf.v35.game.buy

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.game.operation.GraftResolver
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.buy.BuyChoice
import dugsolutions.leaf.v35.player.decision.buy.BuyDieResource
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.buy.BuyPayment
import dugsolutions.leaf.v35.player.decision.buy.BuyStrategy
import dugsolutions.leaf.v35.player.decision.buy.ChoosePaymentRequest
import dugsolutions.leaf.v35.player.decision.buy.ChoosePurchaseRequest
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuyCoordinatorTest {

    @Test
    fun execute_processesPlayersInBuyOrderAndAllowsImmediateDone() {
        val calls = mutableListOf<Int>()
        val firstStrategy = strategy(
            purchase = { calls.add(1); BuyChoice.Done }
        )
        val secondStrategy = strategy(
            purchase = { calls.add(2); BuyChoice.Done }
        )
        val first = player(1, listOf(die(20, 15)), firstStrategy)
        val second = player(2, listOf(die(20, 20)), secondStrategy)
        val fixture = fixture(first, second)

        val result = fixture.coordinator.execute(fixture.game)

        assertEquals(listOf(2, 1), result.order.map { it.value })
        assertEquals(listOf(2, 1), calls)
        assertTrue(result.purchases.isEmpty())
    }

    @Test
    fun execute_playerCanMakeMultiplePurchasesAndOptionsRegenerate() {
        var purchaseCall = 0
        val buyerStrategy = strategy(
            purchase = { request ->
                purchaseCall++
                when (purchaseCall) {
                    1 -> BuyChoice.Purchase(request.options.filterIsInstance<BuyItem.Die>().first { it.sides == DieSides.D6 })
                    2 -> BuyChoice.Purchase(request.options.filterIsInstance<BuyItem.Die>().first { it.sides == DieSides.D8 })
                    else -> BuyChoice.Done
                }
            },
            payment = { request ->
                BuyPayment(dice = listOf(request.availableDice.first { it.value == request.cost }))
            }
        )
        val buyer = player(1, listOf(die(6, 6), die(8, 8), die(20, 20)), buyerStrategy)
        val fixture = fixture(buyer, player(2, emptyList(), doneStrategy()))

        val result = fixture.coordinator.execute(fixture.game)

        assertEquals(listOf(DieSides.D6, DieSides.D8), result.purchases.map { (it.item as BuyItem.Die).sides })
        assertEquals(8, fixture.game.grove.graftBed.count(DieSides.D6))
        assertEquals(8, fixture.game.grove.graftBed.count(DieSides.D8))
        assertEquals(1, buyer.dice.handSize)
        assertEquals(4, buyer.dice.discardSize)
        assertEquals(3, buyerStrategy.purchaseRequests.size)
        assertFalse(buyerStrategy.paymentRequests[1].availableDice.any { it.value == 6 })
        assertTrue(
            buyerStrategy.purchaseRequests[1].options
                .filterIsInstance<BuyItem.Die>()
                .any { it.sides == DieSides.D8 }
        )
    }

    @Test
    fun execute_diePurchaseAllowsOverpaymentAndRecordsResult() {
        val buyerStrategy = purchaseOnce(
            item = { it.filterIsInstance<BuyItem.Die>().first { die -> die.sides == DieSides.D6 } },
            payment = { BuyPayment(dice = listOf(it.availableDice.single())) }
        )
        val buyer = player(1, listOf(die(20, 9)), buyerStrategy)
        val fixture = fixture(buyer, player(2, emptyList(), doneStrategy()))

        val result = fixture.coordinator.execute(fixture.game)

        val purchase = result.purchases.single()
        assertEquals(6, purchase.cost)
        assertEquals(9, purchase.paymentTotal)
        assertEquals(3, purchase.overpayment)
        assertEquals(2, buyer.dice.discardSize)
        assertEquals(9, buyer.dice.discard.first().value)
        assertEquals(6, buyer.dice.discard.last().sides)
        assertEquals(1, buyer.dice.discard.last().value)
    }

    @Test
    fun execute_spentBoostedCritterUsesCurrentValueAndReturnsNormalForm() {
        val buyerStrategy = purchaseOnce(
            item = { it.filterIsInstance<BuyItem.Die>().first { die -> die.sides == DieSides.D6 } },
            payment = { BuyPayment(critters = listOf(Critter.BOOSTED_BEE, Critter.BEE)) }
        )
        val buyer = player(1, listOf(die(20, 1)), buyerStrategy)
        buyer.critters.add(Critter.BOOSTED_BEE).add(Critter.BEE)
        val fixture = fixture(buyer, player(2, emptyList(), doneStrategy()))
        val groveBees = fixture.game.grove.critters.count(Critter.BEE)

        val result = fixture.coordinator.execute(fixture.game)

        assertEquals(6, result.purchases.single().paymentTotal)
        assertTrue(buyer.critters.isEmpty)
        assertEquals(groveBees + 2, fixture.game.grove.critters.count(Critter.BEE))
        assertEquals(0, fixture.game.grove.critters.count(Critter.BOOSTED_BEE))
    }

    @Test
    fun execute_plantPurchaseDecrementsStackAndGraftsFaceDown() {
        lateinit var selected: BuyItem.Plant
        val buyerStrategy = purchaseOnce(
            item = { options -> options.filterIsInstance<BuyItem.Plant>().first().also { selected = it } },
            payment = { BuyPayment(dice = listOf(it.availableDice.single())) }
        )
        val buyer = player(1, listOf(die(20, 5)), buyerStrategy)
        val fixture = fixture(buyer, player(2, emptyList(), doneStrategy()))

        val result = fixture.coordinator.execute(fixture.game)

        assertEquals(1, result.purchases.size)
        assertEquals(selected.card, buyer.creature.cards.single().card)
        assertTrue(buyer.creature.cards.single().isFaceDown)
        assertEquals(selected.card.quantity - 1, fixture.game.grove.plantMarket.stackFor(selected.card)!!.remaining)
        assertTrue(markerMessages(fixture.game).any { it.startsWith("GRAFT player=1") })
        assertTrue(markerMessages(fixture.game).any { it.startsWith("PURCHASE player=1") })
    }

    @Test
    fun execute_filtersIllegalUnavailableUnaffordableAndD4Items() {
        val observing = doneStrategy()
        val buyer = player(1, listOf(die(20, 6)), observing)
        val fixture = fixture(buyer, player(2, emptyList(), doneStrategy()))
        val unavailable = fixture.game.grove.plantMarket.stacks.first()
        repeat(unavailable.card.quantity) { fixture.game.grove.plantMarket.take(unavailable.card) }
        repeat(9) { fixture.game.grove.graftBed.take(DieSides.D6) }

        fixture.coordinator.execute(fixture.game)

        val options = observing.purchaseRequests.single().options
        assertFalse(options.any { it is BuyItem.Plant && it.card == unavailable.card })
        assertFalse(options.any { it is BuyItem.Plant && it.cost > 6 })
        assertFalse(options.any { it is BuyItem.Die && it.sides == DieSides.D4 })
        assertFalse(options.any { it is BuyItem.Die && it.sides == DieSides.D6 })
        assertFalse(options.any { it.cost > 6 })
    }

    @Test
    fun execute_flowerWithoutLegalPlacementIsNotOffered() {
        val observing = doneStrategy()
        val buyer = player(1, listOf(die(20, 20)), observing)
        val fixture = fixture(buyer, player(2, emptyList(), doneStrategy()))

        fixture.coordinator.execute(fixture.game)

        assertFalse(
            observing.purchaseRequests.single().options
                .filterIsInstance<BuyItem.Plant>()
                .any { it.card.type == dugsolutions.leaf.v35.plant.domain.PlantType.FLOWER }
        )
    }

    @Test
    fun execute_invalidPaymentIsRejectedBeforeAnyMutation() {
        val buyerStrategy = purchaseOnce(
            item = { it.filterIsInstance<BuyItem.Die>().first { die -> die.sides == DieSides.D6 } },
            payment = { BuyPayment(dice = listOf(BuyDieResource(20, 20))) }
        )
        val owned = die(20, 6)
        val buyer = player(1, listOf(owned), buyerStrategy)
        val fixture = fixture(buyer, player(2, emptyList(), doneStrategy()))
        val before = fixture.game.grove.graftBed.count(DieSides.D6)

        assertFailsWith<IllegalStateException> { fixture.coordinator.execute(fixture.game) }

        assertEquals(listOf(owned), buyer.dice.hand)
        assertTrue(buyer.dice.discard.isEmpty())
        assertEquals(before, fixture.game.grove.graftBed.count(DieSides.D6))
    }

    @Test
    fun execute_unofferedItemIsRejectedBeforeMutation() {
        val unavailable = BuyItem.Die(DieSides.D4)
        val buyerStrategy = strategy(purchase = { BuyChoice.Purchase(unavailable) })
        val buyer = player(1, listOf(die(20, 20)), buyerStrategy)
        val fixture = fixture(buyer, player(2, emptyList(), doneStrategy()))

        assertFailsWith<IllegalStateException> { fixture.coordinator.execute(fixture.game) }

        assertEquals(0, fixture.game.grove.graftBed.count(DieSides.D4))
        assertEquals(1, buyer.dice.handSize)
    }

    @Test
    fun execute_duplicateEquivalentDiceCanBothPayOnceAndCannotBeReused() {
        var calls = 0
        val buyerStrategy = strategy(
            purchase = { request ->
                calls++
                if (calls == 1) {
                    BuyChoice.Purchase(request.options.filterIsInstance<BuyItem.Die>().first { it.sides == DieSides.D8 })
                } else BuyChoice.Done
            },
            payment = { BuyPayment(dice = it.availableDice.filter { die -> die.value == 4 }) }
        )
        val buyer = player(1, listOf(die(6, 4), die(6, 4), die(20, 20)), buyerStrategy)
        val fixture = fixture(buyer, player(2, emptyList(), doneStrategy()))

        fixture.coordinator.execute(fixture.game)

        assertEquals(1, buyer.dice.handSize)
        assertEquals(20, buyer.dice.hand.single().value)
        assertEquals(
            2,
            buyerStrategy.paymentRequests.single().availableDice.count { it.value == 4 }
        )
    }

    @Test
    fun execute_spentDieCannotBeReusedForSecondPurchase() {
        var purchaseCalls = 0
        var firstPayment: BuyDieResource? = null
        val buyerStrategy = strategy(
            purchase = { request ->
                purchaseCalls++
                if (purchaseCalls <= 2) {
                    BuyChoice.Purchase(
                        request.options.filterIsInstance<BuyItem.Die>()
                            .first { it.sides == DieSides.D6 }
                    )
                } else BuyChoice.Done
            },
            payment = { request ->
                if (firstPayment == null) {
                    firstPayment = request.availableDice.first { it.value == 6 }
                }
                BuyPayment(dice = listOf(firstPayment!!))
            }
        )
        val buyer = player(1, listOf(die(6, 6), die(8, 8)), buyerStrategy)
        val fixture = fixture(buyer, player(2, emptyList(), doneStrategy()))

        assertFailsWith<IllegalStateException> {
            fixture.coordinator.execute(fixture.game)
        }

        assertEquals(8, fixture.game.grove.graftBed.count(DieSides.D6))
        assertEquals(listOf(8), buyer.dice.hand.map { it.value })
        assertEquals(2, buyer.dice.discardSize)
    }

    @Test
    fun execute_otherPlayersResourcesRemainIsolated() {
        val first = player(1, listOf(die(20, 20)), doneStrategy())
        val secondDie = die(8, 8)
        val second = player(2, listOf(secondDie), doneStrategy())
        val fixture = fixture(first, second)

        fixture.coordinator.execute(fixture.game)

        assertEquals(listOf(secondDie), second.dice.hand)
        assertTrue(second.dice.discard.isEmpty())
    }

    private fun fixture(first: Player, second: Player): Fixture {
        val game = GameEngineTestFixture.game(
            cultivationRounds = 1,
            battleRounds = 0,
            players = listOf(first, second)
        )
        return Fixture(
            game,
            BuyCoordinator(
                graftResolver = GraftResolver(game.chronicle),
                createDie = { FixedDie(it.value, 1) }
            )
        )
    }

    private fun player(id: Int, dice: List<Die>, buy: BuyStrategy): Player = Player(
        id = PlayerId(id),
        decisions = DecisionDirector.baseline().copy(buy = buy),
        dice = PlayerDice(hand = dice)
    )

    private fun doneStrategy() = strategy(purchase = { BuyChoice.Done })

    private fun purchaseOnce(
        item: (List<BuyItem>) -> BuyItem,
        payment: (ChoosePaymentRequest) -> BuyPayment
    ): ScriptedBuyStrategy {
        var called = false
        return strategy(
            purchase = {
                if (called) BuyChoice.Done
                else BuyChoice.Purchase(item(it.options)).also { called = true }
            },
            payment = payment
        )
    }

    private fun strategy(
        purchase: (ChoosePurchaseRequest) -> BuyChoice,
        payment: (ChoosePaymentRequest) -> BuyPayment = { BuyPayment() }
    ) = ScriptedBuyStrategy(purchase, payment)

    private class ScriptedBuyStrategy(
        private val purchase: (ChoosePurchaseRequest) -> BuyChoice,
        private val payment: (ChoosePaymentRequest) -> BuyPayment
    ) : BuyStrategy {
        val purchaseRequests = mutableListOf<ChoosePurchaseRequest>()
        val paymentRequests = mutableListOf<ChoosePaymentRequest>()
        override fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice {
            purchaseRequests.add(request)
            return purchase(request)
        }
        override fun choosePayment(request: ChoosePaymentRequest): BuyPayment {
            paymentRequests.add(request)
            return payment(request)
        }
    }

    private fun markerMessages(game: Game) =
        game.chronicle.entries.filterIsInstance<GameEntry.Marker>().map { it.message }

    private fun die(sides: Int, value: Int): Die = FixedDie(sides, value)

    private class FixedDie(sides: Int, value: Int) : Die(sides) {
        init { adjustTo(value) }
        override fun roll(): Die = this
    }

    private data class Fixture(val game: Game, val coordinator: BuyCoordinator)
}
