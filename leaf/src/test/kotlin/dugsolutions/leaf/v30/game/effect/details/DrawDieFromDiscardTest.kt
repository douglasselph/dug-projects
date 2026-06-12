package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DrawDieFromDiscardTest {
    private companion object {
        const val PLAYER_ID = 1
    }

    @Test
    fun invoke_placesHighestSidedDiscardDieInHand() {
        val chronicle = GameChronicle()
        val d6 = TestDie(6, 2, rollValue = 4)
        val d10 = TestDie(10, 5, rollValue = 7)
        val player = Player(id = PLAYER_ID).apply {
            addDieToDiscard(d6)
            addDieToDiscard(d10)
        }

        val result = DrawDieFromDiscard(chronicle)(
            player = player,
            card = loadCard(),
            placeDie = { die ->
                player.addDieToHand(die)
                true
            }
        )

        assertEquals(d10, result)
        assertEquals(7, d10.value)
        assertEquals(1, d10.rollCount)
        assertTrue(player.diceHand.hasDie(d10))
        assertEquals(listOf(d6), player.diceDiscard.dice)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.DRAW_DIE_FROM_DISCARD, entry.effect)
        assertEquals(listOf(10 to 7), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_placesRolledDieOnBattleGrid() {
        val chronicle = GameChronicle()
        val die = TestDie(12, 5, rollValue = 8)
        val player = playerWithDice(PLAYER_ID, TestDie(8, 6), TestDie(6, 3), TestDie(4, 1)).apply {
            addDieToDiscard(die)
        }
        val battle = setupBattle(player)

        DrawDieFromDiscard(chronicle)(
            player = player,
            card = loadCard(),
            placeDie = { drawn -> battle.add(player, BattleStrikeRow.STRIKE_1, drawn) }
        )

        assertEquals(8, die.value)
        val dice = battle.grid.getSquare(player.id, BattleStrikeRow.STRIKE_1).all
            .filterIsInstance<BattleItem.DieItem>()
            .map { it.die.sides to it.die.value }
        assertEquals(listOf(8 to 6, 12 to 8), dice)
        assertEquals(emptyList(), player.diceDiscard.dice)
    }

    @Test
    fun invoke_whenDiscardIsEmpty_recordsWarning() {
        val chronicle = GameChronicle()
        val player = Player(id = PLAYER_ID)

        val result = DrawDieFromDiscard(chronicle)(
            player = player,
            card = loadCard(),
            placeDie = { true }
        )

        assertEquals(null, result)
        val warning = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(PLAYER_ID, warning.playerId)
    }

    private fun setupBattle(player: Player): Battle {
        return Battle().apply {
            setup(
                listOf(
                    player,
                    playerWithDice(2, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1)),
                    playerWithDice(3, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1)),
                    playerWithDice(4, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1))
                )
            )
        }
    }

    private fun playerWithDice(
        id: Int,
        vararg dice: Die
    ): Player {
        return Player(id = id).apply {
            dice.forEach { addDieToHand(it) }
        }
    }

    private fun loadCard(): GameCard {
        return GameCardRegistry()
            .apply { loadFromCsv(Commons.CARD_LIST) }
            .getAllCards()
            .first()
            .copy(effect = CardEffect.DRAW_DIE_FROM_DISCARD)
    }

    private class TestDie(
        sides: Int,
        value: Int,
        private val rollValue: Int = 1
    ) : Die(sides) {
        var rollCount = 0

        init {
            adjustTo(value)
        }

        override fun roll(): Die {
            rollCount++
            adjustTo(rollValue)
            return this
        }
    }
}
