package dugsolutions.leaf.v30.game.effect.details

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

class DrawTwoDiceTest {
    private companion object {
        const val PLAYER_ID = 1
    }

    @Test
    fun invoke_drawsTwoDiceAndChroniclesThem() {
        val chronicle = GameChronicle()
        val player = Player(id = PLAYER_ID).apply {
            addDieToSupply(TestDie(8, 3))
            addDieToSupply(TestDie(6, 2))
        }

        val result = DrawTwoDice(chronicle)(
            player = player,
            card = loadCard(),
            placeDie = { _, _ -> true }
        )

        assertEquals(listOf(6, 8), result.map { it.sides })
        assertEquals(listOf(6, 8), player.diceHand.dice.map { it.sides })
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.DRAW_TWO_DICE, entry.effect)
        assertEquals(listOf(6 to 2, 8 to 3), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_whenOnlyOneDieAvailable_placesAndChroniclesOneDie() {
        val chronicle = GameChronicle()
        val player = Player(id = PLAYER_ID).apply {
            addDieToSupply(TestDie(10, 4))
        }

        val result = DrawTwoDice(chronicle)(
            player = player,
            card = loadCard(),
            placeDie = { _, _ -> true }
        )

        assertEquals(listOf(10), result.map { it.sides })
        assertEquals(2, chronicle.getEntries().size)
        assertIs<GameEntry.Warning>(chronicle.getEntries().first())
        val effect = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().last())
        assertEquals(listOf(10 to 4), effect.dice.map { it.sides to it.value })
    }

    private fun loadCard(): GameCard {
        return GameCardRegistry()
            .apply { loadFromCsv(Commons.CARD_LIST) }
            .getAllCards()
            .first()
            .copy(effect = CardEffect.DRAW_TWO_DICE)
    }

    private class TestDie(
        sides: Int,
        value: Int
    ) : Die(sides) {
        init {
            adjustTo(value)
        }

        override fun roll(): Die = this
    }
}
