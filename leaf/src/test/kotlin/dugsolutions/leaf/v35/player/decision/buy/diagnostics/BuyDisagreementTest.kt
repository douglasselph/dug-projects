package dugsolutions.leaf.v35.player.decision.buy.diagnostics

import dugsolutions.leaf.v35.player.decision.buy.BuyChoice
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BuyDisagreementTest {
    private val d12 = BuyChoice.Purchase(BuyItem.Die(DieSides.D12))
    private val d20 = BuyChoice.Purchase(BuyItem.Die(DieSides.D20))

    @Test fun `classifies matching, differing and done choices`() {
        assertEquals(BuyDisagreementKind.BOTH_DONE, BuyDisagreement.compare(BuyChoice.Done, BuyChoice.Done).kind)
        assertEquals(BuyDisagreementKind.SAME_ITEM, BuyDisagreement.compare(d12, d12).kind)
        assertEquals(BuyDisagreementKind.DIFFERENT_ITEM, BuyDisagreement.compare(d12, d20).kind)
        assertEquals(BuyDisagreementKind.EXPERT_BUYS_BASELINE_DONE, BuyDisagreement.compare(BuyChoice.Done, d12).kind)
        assertEquals(BuyDisagreementKind.BASELINE_BUYS_EXPERT_DONE, BuyDisagreement.compare(d12, BuyChoice.Done).kind)
    }

    @Test fun `card denominators are conditional on same-state recommendations`() {
        // Name matching is deliberately independent of raw card IDs; use selection-level observations.
        val observations = listOf(
            BuyRecommendationComparison(BuySelection.Plant("Queen's Blossom", "old"), BuySelection.Plant("Queen's Blossom", "new"), BuyDisagreementKind.SAME_ITEM),
            BuyRecommendationComparison(BuySelection.Plant("Queen's Blossom", "id"), BuySelection.Die(20), BuyDisagreementKind.DIFFERENT_ITEM),
            BuyRecommendationComparison(BuySelection.Die(20), BuySelection.Plant("Queen's Blossom", "id"), BuyDisagreementKind.DIFFERENT_ITEM),
            BuyRecommendationComparison(BuySelection.Die(12), BuySelection.Die(20), BuyDisagreementKind.DIFFERENT_ITEM)
        )
        val counts = observations.fold(NamedCardChoiceCounts()) { tally, observation -> tally.observe(observation, "Queen's Blossom") }
        assertEquals(1L, counts.both)
        assertEquals(1L, counts.baselineOnly)
        assertEquals(1L, counts.expertOnly)
        assertEquals(1L, counts.neither)
        assertEquals(2.0 / 3.0, counts.disagreementWhenEither!!, 1e-9)
        assertEquals(0.5, counts.expertRejectionGivenBaseline!!, 1e-9)
        assertEquals(0.5, counts.expertOnlyGivenExpert!!, 1e-9)
        assertNull(NamedCardChoiceCounts().disagreementWhenEither)
    }
}
