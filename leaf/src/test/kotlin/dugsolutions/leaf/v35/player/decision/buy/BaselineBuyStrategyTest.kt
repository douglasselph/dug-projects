package dugsolutions.leaf.v35.player.decision.buy

import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BaselineBuyStrategyTest {
    private val strategy = BaselineBuyStrategy()

    @Test
    fun choosePurchase_returnsFirstLegalOption() {
        val options = listOf(BuyItem.Die(DieSides.D8), BuyItem.Die(DieSides.D6))

        assertEquals(BuyChoice.Purchase(options.first()), strategy.choosePurchase(ChoosePurchaseRequest(options)))
    }

    @Test
    fun choosePurchase_whenNoOptions_returnsDone() {
        assertEquals(BuyChoice.Done, strategy.choosePurchase(ChoosePurchaseRequest(emptyList())))
    }

    @Test
    fun choosePayment_choosesSmallestSufficientTotalDeterministically() {
        val request = ChoosePaymentRequest(
            item = BuyItem.Die(DieSides.D8),
            availableDice = listOf(
                BuyDieResource(6, 6),
                BuyDieResource(4, 4),
                BuyDieResource(10, 9)
            ),
            availableCritters = listOf(Critter.WORM, Critter.BEE)
        )

        val first = strategy.choosePayment(request)
        val second = strategy.choosePayment(request)

        assertEquals(8, first.total)
        assertEquals(first.dice, second.dice)
        assertEquals(first.critters, second.critters)
        assertEquals(2, first.dice.size + first.critters.size)
    }

    @Test
    fun choosePayment_preservesDuplicateResourcesWithoutReusingThem() {
        val request = ChoosePaymentRequest(
            item = BuyItem.Die(DieSides.D8),
            availableDice = listOf(BuyDieResource(6, 4), BuyDieResource(6, 4)),
            availableCritters = emptyList()
        )

        val payment = strategy.choosePayment(request)

        assertEquals(8, payment.total)
        assertEquals(2, payment.dice.size)
    }

    @Test
    fun requestsDefensivelyCopyResourcesAndOptions() {
        val options = mutableListOf<BuyItem>(BuyItem.Die(DieSides.D6))
        val dice = mutableListOf(BuyDieResource(6, 6))
        val critters = mutableListOf(Critter.BEE)
        val purchase = ChoosePurchaseRequest(options)
        val payment = ChoosePaymentRequest(options.single(), dice, critters)

        options.clear(); dice.clear(); critters.clear()

        assertEquals(1, purchase.options.size)
        assertEquals(1, payment.availableDice.size)
        assertEquals(1, payment.availableCritters.size)
        assertTrue(strategy.choosePayment(payment).total >= payment.cost)
    }
}
