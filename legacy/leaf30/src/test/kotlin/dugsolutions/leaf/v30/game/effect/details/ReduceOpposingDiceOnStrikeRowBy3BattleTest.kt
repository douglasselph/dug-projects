package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.Battle
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

class ReduceOpposingDiceOnStrikeRowBy3BattleTest {

    @Test
    fun invoke_reducesOnlyOpposingDiceOnTargetStrikeRow() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val actingDie = TestDie(12, 7)
        val opponentDie1 = TestDie(10, 8)
        val opponentDie2 = TestDie(8, 2)
        val opponentOtherRow = TestDie(6, 6)
        val player = playerWithDice(1, actingDie, TestDie(6, 2), TestDie(4, 1))
        val opponent1 = playerWithDice(2, opponentDie1, opponentOtherRow, TestDie(4, 1))
        val opponent2 = playerWithDice(3, opponentDie2, TestDie(6, 1), TestDie(4, 1))
        val opponent3 = playerWithDice(4, TestDie(8, 1), TestDie(6, 1), TestDie(4, 1))
        val battle = Battle().apply {
            setup(listOf(player, opponent1, opponent2, opponent3))
        }

        ReduceOpposingDiceOnStrikeRowBy3Battle(chronicle)(
            battle = battle,
            player = player,
            card = card,
            row = BattleStrikeRow.STRIKE_1
        )

        assertEquals(7, actingDie.value)
        assertEquals(5, opponentDie1.value)
        assertEquals(1, opponentDie2.value)
        assertEquals(6, opponentOtherRow.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.REDUCE_OPPOSING_DICE_ON_STRIKE_ROW_BY_3, entry.effect)
        assertEquals(listOf(4 to 1, 8 to 1, 10 to 5), entry.dice.map { it.sides to it.value })
    }

    private fun loadCard(): GameCard {
        return GameCardRegistry()
            .apply { loadFromCsv(Commons.CARD_LIST) }
            .getAllCards()
            .first()
            .copy(effect = CardEffect.REDUCE_OPPOSING_DICE_ON_STRIKE_ROW_BY_3)
    }

    private fun playerWithDice(
        id: Int,
        vararg dice: Die
    ): Player {
        return Player(id = id).apply {
            dice.forEach { addDieToHand(it) }
        }
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
