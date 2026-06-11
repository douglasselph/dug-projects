package dugsolutions.leaf.v30.battle

import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.battle.domain.Result
import dugsolutions.leaf.v30.battle.domain.StrikeRowResult
import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.domain.CreatureCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BattleAwardWinnersTest {

    @Test
    fun invoke_awardsWinnerBaseVpPlusOnePerWoundedAndAppliesWounds() {
        val chronicle = GameChronicle(currentRound = { 4 })
        val card = loadGameCard()
        val winner = Player(chronicle = chronicle, id = 1)
        val woundedFaceUp = Player(chronicle = chronicle, id = 2).apply {
            addCardToCreature(CreatureCard(card, CreatureCard.Facing.FACE_UP))
        }
        val woundedFaceDown = Player(chronicle = chronicle, id = 3).apply {
            addCardToCreature(CreatureCard(card, CreatureCard.Facing.FACE_DOWN))
        }
        val result = result(
            BattleStrikeRow.STRIKE_1,
            winners = listOf(1),
            wounded = listOf(2, 3)
        )
        val sut = BattleAwardWinners(chronicle)

        sut(listOf(winner, woundedFaceUp, woundedFaceDown), result)

        assertEquals(4, winner.vp)
        assertEquals(true, woundedFaceUp.creatureCards.single().isFaceDown)
        assertEquals(emptyList(), woundedFaceDown.creatureCards)
        val entries = chronicle.getEntries()
        val vpEntry = assertIs<GameEntry.VpAward>(entries[0])
        assertEquals(1, vpEntry.playerId)
        assertEquals(4, vpEntry.amount)
        assertEquals(BattleStrikeRow.STRIKE_1, vpEntry.row)
        assertEquals(4, vpEntry.time.round)
        assertEquals(2, assertIs<GameEntry.WoundCard>(entries[1]).playerId)
        assertEquals(true, assertIs<GameEntry.WoundCard>(entries[1]).wasFlipped)
        assertEquals(3, assertIs<GameEntry.WoundCard>(entries[2]).playerId)
        assertEquals(true, assertIs<GameEntry.WoundCard>(entries[2]).wasLost)
    }

    @Test
    fun invoke_whenMultipleWinners_awardsEachWinner() {
        val chronicle = GameChronicle()
        val player1 = Player(chronicle = chronicle, id = 1)
        val player2 = Player(chronicle = chronicle, id = 2)
        val result = result(
            BattleStrikeRow.STRIKE_2,
            winners = listOf(1, 2),
            wounded = emptyList()
        )
        val sut = BattleAwardWinners(chronicle)

        sut(listOf(player1, player2), result)

        assertEquals(2, player1.vp)
        assertEquals(2, player2.vp)
        assertEquals(2, chronicle.getEntries().filterIsInstance<GameEntry.VpAward>().size)
    }

    private fun result(
        activeRow: BattleStrikeRow,
        winners: List<Int>,
        wounded: List<Int>
    ): Result {
        return Result(
            BattleStrikeRow.entries.associateWith { row ->
                if (row == activeRow) {
                    StrikeRowResult(row, winners, wounded)
                } else {
                    StrikeRowResult(row, emptyList(), emptyList())
                }
            }
        )
    }

    private fun loadGameCard(): GameCard {
        return GameCardRegistry()
            .apply { loadFromCsv(Commons.CARD_LIST) }
            .getAllCards()
            .first()
    }
}
