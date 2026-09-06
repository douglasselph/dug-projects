package dugsolutions.leaf.v35.player.decision.baseline.common

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PurchaseThresholdHeuristicsTest {
    private val costs = listOf(17, 5, 11, 7, 14, 9, 11)

    @Test
    fun tiers_areSortedUniqueAndPositive() {
        assertEquals(
            listOf(5, 7, 9, 11, 14, 17),
            PurchaseThresholdHeuristics.tiers(costs + listOf(0, -1))
        )
    }

    @Test
    fun beforeAndAfter_reportBestAffordableTier() {
        assertEquals(9, PurchaseThresholdHeuristics.bestAffordableTierBefore(10, costs))
        assertEquals(14, PurchaseThresholdHeuristics.bestAffordableTierAfter(10, 5, costs))
    }

    @Test
    fun change_reportsEveryTierCrossedUpward() {
        val change = PurchaseThresholdHeuristics.change(
            beforePower = 10,
            afterPower = 15,
            costs = costs
        )

        assertEquals(9, change.beforeTier)
        assertEquals(14, change.afterTier)
        assertEquals(listOf(11, 14), change.crossedUp)
        assertEquals(emptyList(), change.crossedDown)
        assertEquals(2, change.tierSteps)
        assertTrue(change.improved)
        assertFalse(change.worsened)
    }

    @Test
    fun thresholdBonus_isSignedForGainedOrLostTiers() {
        assertEquals(
            20,
            PurchaseThresholdHeuristics.thresholdBonus(10, 15, costs, pointsPerTier = 10)
        )
        assertEquals(
            -20,
            PurchaseThresholdHeuristics.thresholdBonus(15, 10, costs, pointsPerTier = 10)
        )
    }
}
