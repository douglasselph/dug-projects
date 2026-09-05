package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

/** Assertions for the structured v35 Chronicle. */
object ChronicleAssertions {

    inline fun <reified T : GameEntry> entriesOfType(
        entries: List<GameEntry>
    ): List<T> = ChronicleQueries.ofType(entries)

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

    inline fun <reified T : GameEntry> assertSingle(
        entries: List<GameEntry>,
        noinline predicate: (T) -> Boolean = { true }
    ): T {
        val matches = entries.filterIsInstance<T>().filter(predicate)
        assertEquals(
            1,
            matches.size,
            "Expected exactly one ${T::class.simpleName}; matches=$matches\nAll entries:\n" +
                entries.joinToString("\n")
        )
        return matches.single()
    }

    inline fun <reified T : GameEntry> assertCount(
        entries: List<GameEntry>,
        expected: Int,
        noinline predicate: (T) -> Boolean = { true }
    ) {
        val matches = entries.filterIsInstance<T>().filter(predicate)
        assertEquals(expected, matches.size, "${T::class.simpleName} count; matches=$matches")
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

    fun assertRoundRevealed(
        entries: List<GameEntry>,
        roundNumber: Int,
        cardName: String,
        cardType: RoundCardType
    ): GameEntry.RoundRevealed {
        val reveal = assertSingle<GameEntry.RoundRevealed>(entries) {
            it.roundNumber == roundNumber
        }
        assertEquals(cardName, reveal.cardName)
        assertEquals(cardType, reveal.cardType)
        return reveal
    }

    fun assertRoundCompleted(
        entries: List<GameEntry>,
        roundNumber: Int,
        cardName: String,
        cardType: RoundCardType
    ): GameEntry.RoundCompleted {
        val completed = assertSingle<GameEntry.RoundCompleted>(entries) {
            it.roundNumber == roundNumber
        }
        assertEquals(cardName, completed.cardName)
        assertEquals(cardType, completed.cardType)
        return completed
    }

    fun assertRoundLifecycle(
        entries: List<GameEntry>,
        roundNumber: Int,
        cardName: String,
        cardType: RoundCardType
    ) {
        val reveal = assertRoundRevealed(entries, roundNumber, cardName, cardType)
        val completed = assertRoundCompleted(entries, roundNumber, cardName, cardType)
        assertBefore(reveal, completed)
    }

    fun assertBefore(first: GameEntry, second: GameEntry) {
        assertTrue(
            first.sequence < second.sequence,
            "Expected $first before $second"
        )
    }

    fun assertNoMarkers(entries: List<GameEntry>) {
        assertDoesNotContain<GameEntry.Marker>(entries)
    }

    /** Marker remains useful for explicit test/diagnostic breadcrumbs. */
    fun markerMessages(entries: List<GameEntry>): List<String> =
        entries.filterIsInstance<GameEntry.Marker>().map { it.message }

    fun assertSequenceContinuous(entries: List<GameEntry>) {
        val expected = (1L..entries.size.toLong()).toList()
        assertEquals(expected, entries.map { it.sequence })
    }

    fun assertOnlyRevealRecorded(
        entries: List<GameEntry>,
        roundNumber: Int,
        cardName: String,
        cardType: RoundCardType
    ): GameEntry.RoundRevealed {
        val reveal = assertRoundRevealed(entries, roundNumber, cardName, cardType)
        assertEquals(1, entries.size, "Reveal-only Chronicle should contain one entry")
        return reveal
    }
}
