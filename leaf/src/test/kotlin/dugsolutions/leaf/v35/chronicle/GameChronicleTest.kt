package dugsolutions.leaf.v35.chronicle

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.BuyOrderCritterSnapshot
import dugsolutions.leaf.v35.chronicle.domain.BuyOrderDieSnapshot
import dugsolutions.leaf.v35.chronicle.domain.BuyOrderResourceSnapshot
import dugsolutions.leaf.v35.chronicle.domain.ChronicleRollRewardPolicy
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.chronicle.domain.RollReason
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.MainActionKind
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.tokens.Critter
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

        val dice = mutableListOf(BuyOrderDieSnapshot(DieSides.D8, 7))
        val critters = mutableListOf(BuyOrderCritterSnapshot(Critter.BEE, 2))
        val resources = mutableListOf(
            BuyOrderResourceSnapshot(PlayerId(2), dice, critters)
        )
        val recorded = chronicle.record(
            Moment.BuyOrder(
                order = order,
                resources = resources
            )
        )
        order.clear()
        dice.clear()
        critters.clear()
        resources.clear()

        val entry = recorded as GameEntry.BuyOrder
        assertEquals(listOf(PlayerId(2), PlayerId(1)), entry.order)
        assertEquals(
            BuyOrderResourceSnapshot(
                PlayerId(2),
                listOf(BuyOrderDieSnapshot(DieSides.D8, 7)),
                listOf(BuyOrderCritterSnapshot(Critter.BEE, 2))
            ),
            entry.resources.single()
        )
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
    fun record_preservesMainActionDecisionProbabilityMetadata() {
        val chronicle = GameChronicle()

        val recorded = chronicle.record(
            Moment.MainAction(
                playerId = PlayerId(2),
                phase = ChroniclePhase.CULTIVATION,
                action = MainActionKind.ROUND_EFFECT_1,
                actionNumber = 1,
                decisionProbabilityPercent = 75
            )
        )

        assertEquals(
            GameEntry.MainAction(
                sequence = 1L,
                playerId = PlayerId(2),
                phase = ChroniclePhase.CULTIVATION,
                action = MainActionKind.ROUND_EFFECT_1,
                actionNumber = 1,
                battleStage = null,
                decisionProbabilityPercent = 75
            ),
            recorded
        )
    }


    @Test
    fun record_preservesDieValueChangeData() {
        val chronicle = GameChronicle()

        val recorded = chronicle.record(
            Moment.DieValueChanged(
                playerId = PlayerId(3),
                effect = GameEffect.RAISE_DIE_PLUS_3,
                sides = DieSides.D4,
                before = 1,
                after = 4
            )
        )

        assertEquals(
            GameEntry.DieValueChanged(
                sequence = 1L,
                playerId = PlayerId(3),
                effect = GameEffect.RAISE_DIE_PLUS_3,
                sides = DieSides.D4,
                before = 1,
                after = 4
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
    @Test
    fun scoped_commitsParentBeforeBufferedChildrenWithHierarchyDepth() {
        val chronicle = GameChronicle()

        chronicle.scoped(Moment.Marker("parent")) {
            chronicle.record(Moment.Marker("child"))
            chronicle.scoped(Moment.Marker("grandchild-parent")) {
                chronicle.record(Moment.Marker("grandchild"))
            }
        }

        assertEquals(
            listOf("parent", "child", "grandchild-parent", "grandchild"),
            chronicle.entries.map { (it as GameEntry.Marker).message }
        )
        assertEquals(listOf(0, 1, 1, 2), chronicle.entries.map { it.hierarchyDepth })
        assertEquals(listOf(1L, 2L, 3L, 4L), chronicle.entries.map { it.sequence })
    }

    @Test
    fun scoped_whenBlockFails_discardsParentChildrenAndReservedSequences() {
        val chronicle = GameChronicle()
        chronicle.record(Moment.Marker("before"))

        assertFailsWith<IllegalStateException> {
            chronicle.scoped(Moment.Marker("parent")) {
                chronicle.record(Moment.Marker("child"))
                error("boom")
            }
        }

        val after = chronicle.record(Moment.Marker("after"))
        assertEquals(listOf("before", "after"), chronicle.entries.map { (it as GameEntry.Marker).message })
        assertEquals(2L, after.sequence)
    }

    @Test
    fun scoped_deferredParentCanUseDataComputedByBlock() {
        val chronicle = GameChronicle()
        var result = "not-set"

        chronicle.scoped(parent = { Moment.Marker("parent:$result") }) {
            chronicle.record(Moment.Marker("child"))
            result = "done"
        }

        assertEquals("parent:done", (chronicle.entries[0] as GameEntry.Marker).message)
        assertEquals("child", (chronicle.entries[1] as GameEntry.Marker).message)
    }

    @Test
    fun scoped_whenDeferredParentFails_restoresOuterScopeAndSequence() {
        val chronicle = GameChronicle()

        chronicle.scoped(Moment.Marker("outer")) {
            chronicle.record(Moment.Marker("before nested"))
            assertFailsWith<IllegalStateException> {
                chronicle.scoped(parent = { error("parent failed") }) {
                    chronicle.record(Moment.Marker("discarded nested child"))
                }
            }
            chronicle.record(Moment.Marker("after nested"))
        }

        assertEquals(
            listOf("outer", "before nested", "after nested"),
            chronicle.entries.map { (it as GameEntry.Marker).message }
        )
        assertEquals(listOf(1L, 2L, 3L), chronicle.entries.map { it.sequence })
        assertEquals(listOf(0, 1, 1), chronicle.entries.map { it.hierarchyDepth })
    }

}
