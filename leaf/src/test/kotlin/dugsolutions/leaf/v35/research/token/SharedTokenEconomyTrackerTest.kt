package dugsolutions.leaf.v35.research.token

import dugsolutions.leaf.v35.tokens.Butterflies
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.Critters
import dugsolutions.leaf.v35.tokens.SharedTokenResource
import dugsolutions.leaf.v35.tokens.Token
import dugsolutions.leaf.v35.tokens.Tokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedTokenEconomyTrackerTest {

    @Test
    fun waterGainEmptyAttemptAndReturnAreObservedWithoutChangingInventorySemantics() {
        val tracker = tracker(water = 1)
        val tokens = Tokens(waterCount = 1, supplyObserver = tracker)

        assertNotNull(tokens.pull(Token.WATER))
        assertEquals(0, tokens.waterCount)
        assertNull(tokens.pull(Token.WATER))
        tokens.add(Token.WATER)
        assertEquals(1, tokens.waterCount)

        val summary = tracker.snapshot(SharedTokenResource.WATER)
        assertEquals(1, summary.startingSupply)
        assertEquals(2, summary.gainAttempts)
        assertEquals(1, summary.successfulGains)
        assertEquals(1, summary.failedEmptyGains)
        assertEquals(1, summary.returnsToGrove)
        assertEquals(0, summary.minimumGroveSupply)
        assertEquals(1, summary.maximumOutsideGrove)
        assertTrue(summary.reachedZero)
        assertEquals(1, summary.timesReachedZero)
        assertEquals(2, summary.emptySupplyObservations)
        assertEquals(1, summary.trackedFinalGroveSupply)
    }

    @Test
    fun sunlightMulchCrittersAndButterfliesUseTheSameGenericAccounting() {
        val tracker = tracker(sunlight = 1, mulch = 1, bee = 1, worm = 1, butterfly = 1)
        val tokens = Tokens(
            sunlightCount = 1,
            mulchTokens = listOf(Token.MULCH()),
            supplyObserver = tracker
        )
        val critters = Critters(listOf(Critter.BEE, Critter.WORM), supplyObserver = tracker)
        val butterflies = Butterflies(listOf(Butterfly.GREEN), supplyObserver = tracker)

        assertNotNull(tokens.pull(Token.SUNLIGHT))
        assertNotNull(tokens.pull(Token.MULCH()))
        assertTrue(critters.remove(Critter.BEE))
        assertTrue(critters.remove(Critter.WORM))
        assertTrue(butterflies.remove(Butterfly.GREEN))

        listOf(
            SharedTokenResource.SUNLIGHT,
            SharedTokenResource.MULCH,
            SharedTokenResource.BEE,
            SharedTokenResource.WORM,
            SharedTokenResource.BUTTERFLY
        ).forEach { resource ->
            val summary = tracker.snapshot(resource)
            assertEquals(1, summary.successfulGains, resource.name)
            assertEquals(0, summary.minimumGroveSupply, resource.name)
            assertTrue(summary.reachedZero, resource.name)
        }
    }

    @Test
    fun observerDoesNotChangeTokenOperationsOrIntroduceAnyRandomChoice() {
        val tracker = tracker(water = 2, sunlight = 2, mulch = 2)
        val observed = Tokens(
            waterCount = 2,
            sunlightCount = 2,
            mulchTokens = List(2) { Token.MULCH() },
            supplyObserver = tracker
        )
        val ordinary = Tokens(
            waterCount = 2,
            sunlightCount = 2,
            mulchTokens = List(2) { Token.MULCH() }
        )

        assertEquals(ordinary.pull(Token.WATER), observed.pull(Token.WATER))
        assertEquals(ordinary.pull(Token.SUNLIGHT), observed.pull(Token.SUNLIGHT))
        assertEquals(ordinary.pull(Token.MULCH()), observed.pull(Token.MULCH()))
        ordinary.add(Token.WATER)
        observed.add(Token.WATER)

        assertEquals(ordinary.waterCount, observed.waterCount)
        assertEquals(ordinary.sunlightCount, observed.sunlightCount)
        assertEquals(ordinary.mulchCount, observed.mulchCount)
        assertFalse(tracker.snapshot(SharedTokenResource.WATER).gainAttempts == 0)
    }

    private fun tracker(
        water: Int = 0,
        sunlight: Int = 0,
        mulch: Int = 0,
        bee: Int = 0,
        worm: Int = 0,
        butterfly: Int = 0
    ): SharedTokenEconomyTracker = SharedTokenEconomyTracker().also { tracker ->
        tracker.reset(
            mapOf(
                SharedTokenResource.WATER to water,
                SharedTokenResource.SUNLIGHT to sunlight,
                SharedTokenResource.MULCH to mulch,
                SharedTokenResource.BEE to bee,
                SharedTokenResource.WORM to worm,
                SharedTokenResource.BUTTERFLY to butterfly
            )
        )
    }

    private fun SharedTokenEconomyTracker.snapshot(resource: SharedTokenResource): SharedTokenEconomySnapshot =
        snapshots().single { it.resource == resource }
}
