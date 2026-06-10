package dugsolutions.leaf.v30.chronicle

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.chronicle.domain.EntryKind
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.player.Player
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameChronicleTest {

    @Test
    fun invoke_whenMissingWormWarning_recordsWarningEntry() {
        val card = loadCard()
        val player = Player(id = 7)
        val chronicle = GameChronicle(currentRound = { 3 })

        val entry = chronicle(
            Moment.Warning(
                player = player,
                type = WarningType.MISSING_WORM,
                card = card
            )
        )

        val warning = assertIs<GameEntry.Warning>(entry)
        assertEquals(1L, warning.sequence)
        assertEquals(3, warning.time.round)
        assertEquals(EntryKind.WARNING, warning.kind)
        assertEquals(7, warning.playerId)
        assertEquals(WarningType.MISSING_WORM, warning.type)
        assertEquals(card.id, warning.cardId)
        assertEquals(card.name, warning.cardName)
        assertEquals(listOf(warning), chronicle.getEntries())
    }

    @Test
    fun getNewEntries_returnsEntriesSinceLastCall() {
        val card = loadCard()
        val player = Player(id = 3)
        val chronicle = GameChronicle(currentRound = { 1 })

        val first = chronicle(Moment.Warning(player, WarningType.MISSING_WORM, card))

        assertEquals(listOf(first), chronicle.getNewEntries())
        assertTrue(chronicle.getNewEntries().isEmpty())
    }

    @Test
    fun clear_removesEntriesAndResetsSequence() {
        val card = loadCard()
        val player = Player(id = 3)
        val chronicle = GameChronicle(currentRound = { 1 })
        chronicle(Moment.Warning(player, WarningType.MISSING_WORM, card))

        chronicle.clear()
        val entry = chronicle(Moment.Warning(player, WarningType.MISSING_WORM, card))

        assertEquals(1L, entry.sequence)
        assertEquals(listOf(entry), chronicle.getEntries())
    }

    private fun loadCard() = GameCardRegistry().apply {
        loadFromCsv(Commons.CARD_LIST)
    }.getAllCards().first()
}
