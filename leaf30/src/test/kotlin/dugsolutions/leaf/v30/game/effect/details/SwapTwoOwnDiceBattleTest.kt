package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SwapTwoOwnDiceBattleTest {

    @Test
    fun invoke_swapsValuesOfOwnDiceOnIndicatedRows() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val first = TestDie(12, 6)
        val second = TestDie(8, 3)
        val player = playerWithDice(1, first, second, TestDie(6, 1))
        val battle = setupBattle(player)

        SwapTwoOwnDiceBattle(chronicle)(
            battle = battle,
            player = player,
            card = card,
            target = ExecuteTarget(dice = diceOf(TestDie(12, 6), TestDie(8, 3))),
            row = BattleStrikeRow.STRIKE_1,
            row2 = BattleStrikeRow.STRIKE_2
        )

        assertEquals(3, first.value)
        assertEquals(6, second.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.SWAP_TWO_OWN_DICE, entry.effect)
        assertEquals("Swapped own dice values between STRIKE_1 and STRIKE_2", entry.detail)
        assertEquals(listOf(8 to 6, 12 to 3), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_whenSecondRowIsMissing_throws() {
        val player = playerWithDice(1, TestDie(12, 6), TestDie(8, 3), TestDie(6, 1))
        val battle = setupBattle(player)

        assertThrows<Exception> {
            SwapTwoOwnDiceBattle(GameChronicle())(
                battle = battle,
                player = player,
                card = loadCard(),
                target = ExecuteTarget(dice = diceOf(TestDie(12, 6), TestDie(8, 3))),
                row = BattleStrikeRow.STRIKE_1,
                row2 = null
            )
        }
    }

    @Test
    fun invoke_whenDiceAreNotFoundOnPlayersRows_recordsWarningAndDoesNotSwap() {
        val chronicle = GameChronicle()
        val first = TestDie(12, 6)
        val second = TestDie(8, 3)
        val player = playerWithDice(1, first, second, TestDie(6, 1))
        val battle = setupBattle(player)

        SwapTwoOwnDiceBattle(chronicle)(
            battle = battle,
            player = player,
            card = loadCard(),
            target = ExecuteTarget(dice = diceOf(TestDie(20, 6), TestDie(8, 3))),
            row = BattleStrikeRow.STRIKE_1,
            row2 = BattleStrikeRow.STRIKE_2
        )

        assertEquals(6, first.value)
        assertEquals(3, second.value)
        val warning = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.RAISE_DIE_NOT_FOUND, warning.type)
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
            .copy(effect = CardEffect.SWAP_TWO_OWN_DICE)
    }

    private fun diceOf(vararg dice: Die): Dice = Dice(dice.toList())

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
