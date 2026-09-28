package dugsolutions.leaf.v35.player.decision.learned.buy

import dugsolutions.leaf.v35.player.decision.buy.*
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LearnedBuyFoundationTest {
    private class PaymentStub : BuyStrategy {
        override fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice = BuyChoice.Done
        override fun choosePayment(request: ChoosePaymentRequest): BuyPayment = BuyPayment()
    }

    @Test
    fun `feature extraction is deterministic and distinguishes die actions`() {
        val d8a = BuyFeatureExtractor.extract(DecisionContext.EMPTY, BuyItem.Die(DieSides.D8))
        val d8b = BuyFeatureExtractor.extract(DecisionContext.EMPTY, BuyItem.Die(DieSides.D8))
        val d12 = BuyFeatureExtractor.extract(DecisionContext.EMPTY, BuyItem.Die(DieSides.D12))
        assertEquals(d8a.asMap(), d8b.asMap())
        assertEquals(1.0, d8a[BuyFeature.ACTION_D8])
        assertEquals(0.0, d8a[BuyFeature.ACTION_D12])
        assertEquals(1.0, d12[BuyFeature.ACTION_D12])
    }

    @Test
    fun `hand written weights can prefer D12 over D8`() {
        val weights = LearnedBuyWeights.zeros().with(BuyFeature.ACTION_D12, 3.0)
        val strategy = LearnedBuyStrategy(weights, PaymentStub())
        val choice = strategy.choosePurchase(ChoosePurchaseRequest(listOf(BuyItem.Die(DieSides.D8), BuyItem.Die(DieSides.D12))))
        assertEquals(BuyItem.Die(DieSides.D12), assertIs<BuyChoice.Purchase>(choice).item)
    }

    @Test
    fun `Done participates as a scored action`() {
        val weights = LearnedBuyWeights.zeros().with(BuyFeature.ACTION_DONE, 2.0)
        val strategy = LearnedBuyStrategy(weights, PaymentStub())
        assertEquals(BuyChoice.Done, strategy.choosePurchase(ChoosePurchaseRequest(listOf(BuyItem.Die(DieSides.D20)))))
    }

    @Test
    fun `payment remains delegated to baseline-compatible strategy`() {
        var called = false
        val delegate = object : BuyStrategy {
            override fun choosePurchase(request: ChoosePurchaseRequest) = BuyChoice.Done
            override fun choosePayment(request: ChoosePaymentRequest): BuyPayment { called = true; return BuyPayment() }
        }
        LearnedBuyStrategy(LearnedBuyWeights.zeros(), delegate).choosePayment(
            ChoosePaymentRequest(BuyItem.Die(DieSides.D4), emptyList(), emptyList())
        )
        assertTrue(called)
    }

    @Test
    fun `weights save and load exactly`(@TempDir dir: Path) {
        val path = dir.resolve("weights.txt")
        val original = LearnedBuyWeights.zeros().with(BuyFeature.ACTION_D20, 7.25).with(BuyFeature.SELF_PLANT_COUNT, -1.5)
        original.save(path)
        val loaded = LearnedBuyWeights.load(path)
        BuyFeature.entries.forEach { assertEquals(original[it], loaded[it], "weight $it") }
    }

    @Test
    fun `unknown weight key fails loudly`(@TempDir dir: Path) {
        val path = dir.resolve("weights.txt")
        LearnedBuyWeights.zeros().save(path)
        java.nio.file.Files.writeString(path, java.nio.file.Files.readString(path) + "BOGUS=1.0\n")
        val error = assertFailsWith<IllegalArgumentException> { LearnedBuyWeights.load(path) }
        assertTrue(error.message!!.contains("Unknown learned Buy weight keys"))
    }
}
