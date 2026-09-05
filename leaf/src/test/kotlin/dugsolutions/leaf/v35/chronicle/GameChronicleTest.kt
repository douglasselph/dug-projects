package dugsolutions.leaf.v35.chronicle

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.ChronicleRollRewardPolicy
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.chronicle.domain.RollReason
import dugsolutions.leaf.v35.player.PlayerId
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GameChronicleTest {

    @Test
    fun record_assignsSequenceStartingAtOne() {
        // Arrange
        val chronicle = GameChronicle()

        // Act
        val first = chronicle.record(Moment.Marker("first"))
        val second = chronicle.record(Moment.Marker("second"))

        // Assert
        assertEquals(1L, first.sequence)
        assertEquals(2L, second.sequence)
    }

    @Test
    fun record_transformsMomentIntoImmutableEntry() {
        // Arrange
        val chronicle = GameChronicle()

        // Act
        val result = chronicle.record(Moment.Marker("hello"))

        // Assert
        assertEquals(
            GameEntry.Marker(
                sequence = 1L,
                message = "hello"
            ),
            result
        )
    }


    @Test
    fun record_transformsTypedMomentWithoutRetainingMutableInputList() {
        val chronicle = GameChronicle()
        val order = mutableListOf(PlayerId(2), PlayerId(1))

        val recorded = chronicle.record(Moment.BuyOrder(order))
        order.clear()

        val entry = recorded as GameEntry.BuyOrder
        assertEquals(listOf(PlayerId(2), PlayerId(1)), entry.order)
    }

    @Test
    fun record_preservesStructuredDieRollData() {
        val chronicle = GameChronicle()

        val recorded = chronicle.record(
            Moment.DieRolled(
                playerId = PlayerId(3),
                sides = 12,
                value = 7,
                rewardPolicy = ChronicleRollRewardPolicy.NORMAL,
                reason = RollReason.DRAW
            )
        )

        assertEquals(
            GameEntry.DieRolled(
                sequence = 1L,
                playerId = PlayerId(3),
                sides = 12,
                value = 7,
                rewardPolicy = ChronicleRollRewardPolicy.NORMAL,
                reason = RollReason.DRAW
            ),
            recorded
        )
    }

    @Test
    fun entries_returnsRecordedEntriesInSequenceOrder() {
        // Arrange
        val chronicle = GameChronicle()
        val first = chronicle.record(Moment.Marker("one"))
        val second = chronicle.record(Moment.Marker("two"))
        val third = chronicle.record(Moment.Marker("three"))

        // Act
        val result = chronicle.entries

        // Assert
        assertEquals(
            listOf(first, second, third),
            result
        )
    }

    @Test
    fun entries_returnsDefensiveSnapshot() {
        // Arrange
        val chronicle = GameChronicle()
        chronicle.record(Moment.Marker("one"))
        val snapshot = chronicle.entries

        // Act
        chronicle.record(Moment.Marker("two"))

        // Assert
        assertEquals(1, snapshot.size)
        assertEquals(2, chronicle.entries.size)
    }

    @Test
    fun entriesAfter_zero_returnsAllEntries() {
        // Arrange
        val chronicle = GameChronicle()
        val first = chronicle.record(Moment.Marker("one"))
        val second = chronicle.record(Moment.Marker("two"))

        // Act
        val result = chronicle.entriesAfter(0)

        // Assert
        assertEquals(
            listOf(first, second),
            result
        )
    }

    @Test
    fun entriesAfter_returnsOnlyEntriesWithGreaterSequence() {
        // Arrange
        val chronicle = GameChronicle()
        chronicle.record(Moment.Marker("one"))
        chronicle.record(Moment.Marker("two"))
        val third = chronicle.record(Moment.Marker("three"))
        val fourth = chronicle.record(Moment.Marker("four"))

        // Act
        val result = chronicle.entriesAfter(2)

        // Assert
        assertEquals(
            listOf(third, fourth),
            result
        )
    }

    @Test
    fun entriesAfter_whenNoLaterEntries_returnsEmptyList() {
        // Arrange
        val chronicle = GameChronicle()
        chronicle.record(Moment.Marker("one"))

        // Act
        val result = chronicle.entriesAfter(1)

        // Assert
        assertEquals(emptyList(), result)
    }

    @Test
    fun entriesAfter_whenSequenceNegative_throws() {
        // Arrange
        val chronicle = GameChronicle()

        // Act / Assert
        assertFailsWith<IllegalArgumentException> {
            chronicle.entriesAfter(-1)
        }
    }

    @Test
    fun clear_removesEntriesAndResetsSequenceToOne() {
        // Arrange
        val chronicle = GameChronicle()
        chronicle.record(Moment.Marker("one"))
        chronicle.record(Moment.Marker("two"))

        // Act
        chronicle.clear()
        val firstAfterClear = chronicle.record(
            Moment.Marker("after clear")
        )

        // Assert
        assertEquals(1, chronicle.entries.size)
        assertEquals(1L, firstAfterClear.sequence)
    }

    @Test
    fun concurrentRecording_assignsUniqueContiguousSequences() {
        // Arrange
        val chronicle = GameChronicle()
        val threadCount = 8
        val entriesPerThread = 100
        val totalEntries = threadCount * entriesPerThread

        val executor = Executors.newFixedThreadPool(threadCount)
        val start = CountDownLatch(1)
        val done = CountDownLatch(threadCount)

        try {
            repeat(threadCount) { thread ->
                executor.execute {
                    try {
                        start.await()

                        repeat(entriesPerThread) { index ->
                            chronicle.record(
                                Moment.Marker("$thread-$index")
                            )
                        }
                    } finally {
                        done.countDown()
                    }
                }
            }

            // Act
            start.countDown()
            assertTrue(
                done.await(10, TimeUnit.SECONDS),
                "Concurrent Chronicle recording did not finish"
            )

            // Assert
            val entries = chronicle.entries
            assertEquals(totalEntries, entries.size)
            assertEquals(
                (1L..totalEntries.toLong()).toList(),
                entries.map { it.sequence }
            )
        } finally {
            executor.shutdownNow()
        }
    }
}
