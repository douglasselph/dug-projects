package dugsolutions.leaf.integration.v35.sanity.cultivation

import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.DieSpec
import dugsolutions.leaf.integration.v35.support.giveCritter
import dugsolutions.leaf.integration.v35.support.setPlayerDice
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.PurchaseKind
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.buy.BuyPayment
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CultivationBuySanityTest {

    @Test
    fun `Buy Order starts with highest Hand then continues clockwise`() {
        val first = ScriptedDecisionDirector().apply { buy.thenDone() }
        val second = ScriptedDecisionDirector().apply { buy.thenDone() }
        cultivationHarness(first = first, second = second).use { harness ->
            harness.setPlayerDice(1, hand = listOf(DieSpec(DieSides.D6, 6), DieSpec(DieSides.D4, 1)))
            harness.setPlayerDice(2, hand = listOf(DieSpec(DieSides.D6, 5), DieSpec(DieSides.D4, 4)))
            harness.revealNextRound()

            val result = harness.runCultivationBuy()

            assertEquals(listOf(PlayerId(1), PlayerId(2)), result.order)
            assertEquals(result.order, ChronicleQueries.buyOrders(harness.chronicleEntries()).single().order)
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `complete Buy Order tie uses scripted D20 until a leader exists`() {
        val first = ScriptedDecisionDirector().apply { buy.thenDone() }
        val second = ScriptedDecisionDirector().apply { buy.thenDone() }
        val randomizer = ScriptedRandomizer().rolls(4, 17)
        cultivationHarness(randomizer, first = first, second = second).use { harness ->
            harness.setPlayerDice(1, hand = listOf(DieSpec(DieSides.D6, 5), DieSpec(DieSides.D4, 2)))
            harness.setPlayerDice(2, hand = listOf(DieSpec(DieSides.D6, 5), DieSpec(DieSides.D4, 2)))
            harness.revealNextRound()

            val result = harness.runCultivationBuy()

            assertEquals(listOf(PlayerId(2), PlayerId(1)), result.order)
            randomizer.assertExhausted()
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `Plant purchase spends payment decrements stack and grafts face down`() {
        val first = ScriptedDecisionDirector().apply {
            buy.thenPurchasePlant("Root_05_02")
            buy.thenPayment { request ->
                BuyPayment(dice = listOf(request.availableDice.single { it.value == 5 }))
            }
        }
        val second = ScriptedDecisionDirector()
        cultivationHarness(first = first, second = second).use { harness ->
            harness.setPlayerDice(1, hand = listOf(DieSpec(DieSides.D6, 5)))
            harness.setPlayerDice(2)
            harness.revealNextRound()

            val result = harness.runCultivationBuy()
            val snapshot = harness.snapshot()

            assertEquals(1, result.purchases.size)
            assertEquals(PurchaseKind.PLANT, harness.chronicleEntries().filterIsInstance<GameEntry.Purchase>().single().kind)
            assertEquals(5, result.purchases.single().paymentTotal)
            assertEquals(5, snapshot.grove.plantStacks.single { it.name == "Root_05_02" }.remaining)
            assertEquals(listOf("Root_05_02"), snapshot.player(1).plants.map { it.name })
            assertTrue(snapshot.player(1).plants.single().faceUp.not())
            assertEquals(listOf(5), snapshot.player(1).discard.map { it.value })
            assertTrue(harness.chronicleEntries().any { it is GameEntry.Graft && it.plantName == "Root_05_02" })
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `die purchase sends bought die and payment die to Discard`() {
        val first = ScriptedDecisionDirector().apply {
            buy.thenPurchaseDie(DieSides.D6)
            buy.thenPayment { request -> BuyPayment(dice = listOf(request.availableDice.single())) }
        }
        val second = ScriptedDecisionDirector()
        cultivationHarness(first = first, second = second).use { harness ->
            harness.setPlayerDice(1, hand = listOf(DieSpec(DieSides.D8, 6)))
            harness.setPlayerDice(2)
            harness.revealNextRound()

            harness.runCultivationBuy()
            val snapshot = harness.snapshot()

            assertEquals(8, snapshot.grove.graftBed.getValue(DieSides.D6))
            assertEquals(2, snapshot.player(1).discard.size)
            assertTrue(snapshot.player(1).discard.any { it.dieSides == DieSides.D8 && it.value == 6 })
            assertTrue(snapshot.player(1).discard.any { it.dieSides == DieSides.D6 && it.value == 1 })
            val purchase = harness.chronicleEntries().filterIsInstance<GameEntry.Purchase>().single()
            assertEquals(PurchaseKind.DIE, purchase.kind)
            assertEquals("D6", purchase.itemName)
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `Critters alone can pay a purchase and return to Grove`() {
        val first = ScriptedDecisionDirector().apply {
            buy.thenPurchaseDie(DieSides.D6)
            buy.thenPayment { request -> BuyPayment(critters = request.availableCritters) }
        }
        val second = ScriptedDecisionDirector()
        cultivationHarness(first = first, second = second).use { harness ->
            harness.setPlayerDice(1)
            // Give P2 a harmless value-1 die so Buy Order is deterministic
            // without invoking the D20 full-tie breaker.
            harness.setPlayerDice(2, hand = listOf(DieSpec(DieSides.D4, 1)))
            harness.giveCritter(1, Critter.BEE, count = 3)
            harness.revealNextRound()

            val result = harness.runCultivationBuy()
            val snapshot = harness.snapshot()

            assertEquals(6, result.purchases.single().paymentTotal)
            assertEquals(0, snapshot.player(1).bees)
            assertEquals(9, snapshot.grove.bees)
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `mixed die and Critter payment totals their current values`() {
        val first = ScriptedDecisionDirector().apply {
            buy.thenPurchaseDie(DieSides.D8)
            buy.thenPayment { request ->
                BuyPayment(
                    dice = listOf(request.availableDice.single { it.value == 5 }),
                    critters = request.availableCritters
                )
            }
        }
        val second = ScriptedDecisionDirector()
        cultivationHarness(first = first, second = second).use { harness ->
            harness.setPlayerDice(1, hand = listOf(DieSpec(DieSides.D6, 5)))
            harness.setPlayerDice(2)
            harness.giveCritter(1, Critter.BEE)
            harness.giveCritter(1, Critter.WORM)
            harness.revealNextRound()

            val result = harness.runCultivationBuy()
            val snapshot = harness.snapshot()

            assertEquals(8, result.purchases.single().paymentTotal)
            assertEquals(0, snapshot.player(1).bees)
            assertEquals(0, snapshot.player(1).worms)
            assertEquals(8, snapshot.grove.graftBed.getValue(DieSides.D8))
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `overpayment is accepted and recorded without carrying excess forward`() {
        val first = ScriptedDecisionDirector().apply {
            buy.thenPurchaseDie(DieSides.D6)
            buy.thenPayment { request -> BuyPayment(dice = listOf(request.availableDice.single())) }
        }
        val second = ScriptedDecisionDirector()
        cultivationHarness(first = first, second = second).use { harness ->
            harness.setPlayerDice(1, hand = listOf(DieSpec(DieSides.D8, 8)))
            harness.setPlayerDice(2)
            harness.revealNextRound()

            val result = harness.runCultivationBuy()
            val purchase = result.purchases.single()
            val entry = harness.chronicleEntries().filterIsInstance<GameEntry.Purchase>().single()

            assertEquals(6, purchase.cost)
            assertEquals(8, purchase.paymentTotal)
            assertEquals(2, purchase.overpayment)
            assertEquals(2, entry.overpayment)
            assertTrue(harness.snapshot().player(1).hand.isEmpty())
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `player may make multiple fully-paid purchases in one Buy turn`() {
        val first = ScriptedDecisionDirector().apply {
            buy.thenPurchaseDie(DieSides.D6)
            buy.thenPayment { request -> BuyPayment(dice = listOf(request.availableDice.first { it.value == 6 })) }
            buy.thenPurchaseDie(DieSides.D6)
            buy.thenPayment { request -> BuyPayment(dice = listOf(request.availableDice.first { it.value == 6 })) }
        }
        val second = ScriptedDecisionDirector()
        cultivationHarness(first = first, second = second).use { harness ->
            harness.setPlayerDice(
                1,
                hand = listOf(
                    DieSpec(DieSides.D6, 6),
                    DieSpec(DieSides.D6, 6)
                )
            )
            harness.setPlayerDice(2)
            harness.revealNextRound()

            val result = harness.runCultivationBuy()
            val snapshot = harness.snapshot()

            assertEquals(2, result.purchases.size)
            assertEquals(7, snapshot.grove.graftBed.getValue(DieSides.D6))
            assertEquals(4, snapshot.player(1).discard.size)
            assertEquals(2, ChronicleQueries.purchasesFor(harness.chronicleEntries(), PlayerId(1)).size)
            first.assertExhausted()
            second.assertExhausted()
        }
    }
}
