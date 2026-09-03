package dugsolutions.leaf.v35.grove

import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GraftBedTest {

    @Test
    fun newGraftBed_hasV35StartingCounts() {
        val bed = GraftBed()

        assertEquals(0, bed.count(DieSides.D4))
        assertEquals(9, bed.count(DieSides.D6))
        assertEquals(9, bed.count(DieSides.D8))
        assertEquals(9, bed.count(DieSides.D10))
        assertEquals(9, bed.count(DieSides.D12))
        assertEquals(9, bed.count(DieSides.D20))
    }

    @Test
    fun take_whenAvailable_decrementsOnlyRequestedSize() {
        val bed = GraftBed()

        val result = bed.take(DieSides.D8)

        assertTrue(result)
        assertEquals(8, bed.count(DieSides.D8))
        assertEquals(9, bed.count(DieSides.D6))
        assertEquals(9, bed.count(DieSides.D10))
    }

    @Test
    fun take_whenEmpty_returnsFalseWithoutMutation() {
        val bed = GraftBed()

        val result = bed.take(DieSides.D4)

        assertFalse(result)
        assertEquals(0, bed.count(DieSides.D4))
    }

    @Test
    fun take_untilEmpty_thenFailsWithoutGoingNegative() {
        val bed = GraftBed()

        repeat(9) {
            assertTrue(
                bed.take(DieSides.D12)
            )
        }

        assertEquals(
            0,
            bed.count(DieSides.D12)
        )
        assertFalse(
            bed.take(DieSides.D12)
        )
        assertEquals(
            0,
            bed.count(DieSides.D12)
        )
    }

    @Test
    fun returnD4_addsToD4ReturnSpace() {
        val bed = GraftBed()

        bed.returnD4()
        bed.returnD4()

        assertEquals(
            2,
            bed.count(DieSides.D4)
        )
        assertTrue(
            bed.has(DieSides.D4)
        )
    }

    @Test
    fun returnedD4_canLaterBeTaken() {
        val bed = GraftBed()
        bed.returnD4()

        val result =
            bed.take(DieSides.D4)

        assertTrue(result)
        assertEquals(
            0,
            bed.count(DieSides.D4)
        )
    }

    @Test
    fun counts_returnsDefensiveSnapshot() {
        val bed = GraftBed()
        val snapshot = bed.counts

        bed.take(DieSides.D6)

        assertEquals(
            9,
            snapshot.getValue(DieSides.D6)
        )
        assertEquals(
            8,
            bed.count(DieSides.D6)
        )
    }

    @Test
    fun reset_restoresStartingCounts() {
        val bed = GraftBed()

        bed.take(DieSides.D6)
        bed.take(DieSides.D20)
        bed.returnD4()

        bed.reset()

        assertEquals(0, bed.count(DieSides.D4))
        DieSides.entries
            .filterNot { it == DieSides.D4 }
            .forEach {
                assertEquals(
                    9,
                    bed.count(it)
                )
            }
    }
}
