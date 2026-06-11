package dugsolutions.leaf.v30.chronicle

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.chronicle.domain.EntryKind
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.chronicle.domain.GameEntryMessage
import dugsolutions.leaf.v30.chronicle.domain.MainActionType
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.round.RoundCardRegistry
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

    @Test
    fun invoke_whenRoundRevealed_recordsRoundCardEntryAndMessage() {
        val roundCard = loadRoundCard()
        val chronicle = GameChronicle(currentRound = { 2 })

        val entry = assertIs<GameEntry.RoundRevealed>(chronicle(Moment.RoundRevealed(roundCard)))

        assertEquals(EntryKind.ROUND_REVEALED, entry.kind)
        assertEquals(roundCard.id, entry.roundCardId)
        assertEquals(roundCard.name, entry.roundCardName)
        assertTrue(GameEntryMessage()(entry).contains("revealed"))
    }

    @Test
    fun invoke_whenDiceRolled_snapshotsPlayerHandDice() {
        val player = Player(id = 4)
        player.addDieToHand(FixedDie(6, 3))
        player.addDieToHand(FixedDie(8, 5))
        val chronicle = GameChronicle(currentRound = { 2 })

        val entry = assertIs<GameEntry.DiceRolled>(chronicle(Moment.DiceRolled(player)))

        assertEquals(EntryKind.DICE_ROLLED, entry.kind)
        assertEquals(listOf(6 to 3, 8 to 5), entry.dice.map { it.sides to it.value })
        assertTrue(GameEntryMessage()(entry).contains("player 4 rolled dice"))
    }

    @Test
    fun invoke_whenRewardRecordsCritter_formatsRewardMessage() {
        val player = Player(id = 5)
        val die = FixedDie(6, 1)
        val chronicle = GameChronicle(currentRound = { 2 })

        val entry = assertIs<GameEntry.Reward>(
            chronicle(Moment.Reward(player = player, die = die, critter = Critter.BEE))
        )

        assertEquals(EntryKind.REWARD, entry.kind)
        assertEquals(Critter.BEE, entry.critter)
        assertTrue(GameEntryMessage()(entry).contains("received BEE"))
    }

    @Test
    fun invoke_whenMainActionRecordsDie_formatsActionMessage() {
        val player = Player(id = 6)
        val die = FixedDie(10, 7)
        val chronicle = GameChronicle(currentRound = { 2 })

        val entry = assertIs<GameEntry.MainAction>(
            chronicle(
                Moment.MainAction(
                    player = player,
                    action = MainActionType.PULL_DIE,
                    detail = "Pulled and rolled a die",
                    die = die
                )
            )
        )

        assertEquals(EntryKind.MAIN_ACTION, entry.kind)
        assertEquals(MainActionType.PULL_DIE, entry.action)
        assertTrue(GameEntryMessage()(entry).contains("PULL_DIE"))
    }

    private fun loadCard() = GameCardRegistry().apply {
        loadFromCsv(Commons.CARD_LIST)
    }.getAllCards().first()

    private fun loadRoundCard() = RoundCardRegistry().apply {
        loadFromCsv(Commons.ROUND_CARD_LIST)
    }.getAllCards().first()

    private class FixedDie(
        sides: Int,
        value: Int
    ) : Die(sides) {
        init {
            adjustTo(value)
        }

        override fun roll(): Die = this
    }
}
