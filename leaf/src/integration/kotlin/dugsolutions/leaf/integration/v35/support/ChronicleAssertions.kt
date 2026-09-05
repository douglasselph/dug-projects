package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

/** Assertions for the structured v35 Chronicle. */
object ChronicleAssertions {

    inline fun <reified T : GameEntry> entriesOfType(
        entries: List<GameEntry>
    ): List<T> = entries.filterIsInstance<T>()

    inline fun <reified T : GameEntry> assertContains(
        entries: List<GameEntry>,
        noinline predicate: (T) -> Boolean = { true }
    ): T {
        val matches = entries.filterIsInstance<T>()
        val found = matches.firstOrNull(predicate)
        assertTrue(
            found != null,
            "Expected Chronicle entry ${T::class.simpleName}, but entries were:\n" +
                entries.joinToString("\n")
        )
        return requireNotNull(found)
    }

    inline fun <reified T : GameEntry> assertDoesNotContain(
        entries: List<GameEntry>,
        noinline predicate: (T) -> Boolean = { true }
    ) {
        val found = entries.filterIsInstance<T>().firstOrNull(predicate)
        assertFalse(
            found != null,
            "Did not expect Chronicle entry ${T::class.simpleName}, but found $found"
        )
    }

    /** Marker remains useful for explicit test/diagnostic breadcrumbs. */
    fun markerMessages(entries: List<GameEntry>): List<String> =
        entries.filterIsInstance<GameEntry.Marker>().map { it.message }

    fun assertSequenceContinuous(entries: List<GameEntry>) {
        val expected = (1L..entries.size.toLong()).toList()
        assertEquals(expected, entries.map { it.sequence })
    }
}
