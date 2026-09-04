package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Chronicle assertions intentionally match semantic fragments rather than a
 * complete human-readable marker sentence. This keeps early integration tests
 * useful while the Chronicle is still Marker-based.
 */
object ChronicleAssertions {

    fun markerMessages(entries: List<GameEntry>): List<String> =
        entries.filterIsInstance<GameEntry.Marker>().map { it.message }

    fun assertContainsMarker(
        entries: List<GameEntry>,
        vararg fragments: String
    ) {
        val messages = markerMessages(entries)
        assertTrue(
            messages.any { message -> fragments.all(message::contains) },
            "Expected Chronicle marker containing ${fragments.toList()}, " +
                "but markers were:\n${messages.joinToString("\n")}"
        )
    }

    fun assertDoesNotContainMarker(
        entries: List<GameEntry>,
        vararg fragments: String
    ) {
        val messages = markerMessages(entries)
        assertFalse(
            messages.any { message -> fragments.all(message::contains) },
            "Did not expect Chronicle marker containing ${fragments.toList()}, " +
                "but markers were:\n${messages.joinToString("\n")}"
        )
    }

    fun assertMarkersInOrder(
        entries: List<GameEntry>,
        vararg markerFragments: List<String>
    ) {
        val messages = markerMessages(entries)
        var nextIndex = 0

        markerFragments.forEach { fragments ->
            val found = (nextIndex until messages.size).firstOrNull { index ->
                fragments.all(messages[index]::contains)
            }

            assertTrue(
                found != null,
                "Expected Chronicle marker containing $fragments after index $nextIndex, " +
                    "but markers were:\n${messages.joinToString("\n")}"
            )
            nextIndex = requireNotNull(found) + 1
        }
    }

    fun assertSequenceContinuous(entries: List<GameEntry>) {
        val expected = (1L..entries.size.toLong()).toList()
        assertEquals(expected, entries.map { it.sequence })
    }
}
