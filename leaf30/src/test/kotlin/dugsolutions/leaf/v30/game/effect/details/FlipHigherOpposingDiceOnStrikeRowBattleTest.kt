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
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FlipHigherOpposingDiceOnStrikeRowBattleTest {
    private companion object {
        const val PLAYER_ID = 1
    }

    @Test
    fun invoke_flipsHigherOpponentDiceOnly() {
        val chronicle = GameChronicle()
        val ownDie = TestDie(12, 5)
        val higherOpponent = TestDie(10, 7)
        val equalOpponent = TestDie(8, 5)
        val lowerOpponent = TestDie(6, 4)
        val player = playerWithDice(PLAYER_ID, ownDie, TestDie(6, 3), TestDie(4, 1))
        val opponent2 = playerWithDice(2, higherOpponent, TestDie(6, 3), TestDie(4, 1))
        val opponent3 = playerWithDice(3, equalOpponent, TestDie(6, 3), TestDie(4, 1))
        val opponent4 = playerWithDice(4, lowerOpponent, TestDie(8, 3), TestDie(4, 1))
        val battle = setupBattle(player, opponent2, opponent3, opponent4)

        FlipHigherOpposingDiceOnStrikeRowBattle(chronicle)(
            battle = battle,
            player = player,
            card = loadCard(),
            target = ExecuteTarget(dice = diceOf(TestDie(12, 5))),
            row = BattleStrikeRow.STRIKE_1
        )

        assertEquals(4, higherOpponent.value)
        assertEquals(5, equalOpponent.value)
        assertEquals(4, lowerOpponent.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.FLIP_HIGHER_OPPOSING_DICE_ON_STRIKE_ROW, entry.effect)
        assertEquals("Flipped 1 higher opposing die/dice on STRIKE_1", entry.detail)
        assertEquals(listOf(10 to 4), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_whenHigherOpponentDieIsD4_countsItButD4ValueDoesNotChange() {
        val chronicle = GameChronicle()
        val ownDie = TestDie(6, 1)
        val d4Opponent = TestDie(4, 3)
        val player = playerWithDice(PLAYER_ID, ownDie, TestDie(8, 1), TestDie(10, 1))
        val opponent2 = playerWithDice(2, d4Opponent, TestDie(6, 1), TestDie(8, 1))
        val opponent3 = playerWithDice(3, TestDie(8, 1), TestDie(6, 1), TestDie(4, 1))
        val opponent4 = playerWithDice(4, TestDie(10, 1), TestDie(6, 1), TestDie(4, 1))
        val battle = setupBattle(player, opponent2, opponent3, opponent4)

        FlipHigherOpposingDiceOnStrikeRowBattle(chronicle)(
            battle = battle,
            player = player,
            card = loadCard(),
            target = ExecuteTarget(dice = diceOf(TestDie(6, 1))),
            row = BattleStrikeRow.STRIKE_1
        )

        assertEquals(3, d4Opponent.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals("Flipped 1 higher opposing die/dice on STRIKE_1", entry.detail)
        assertEquals(listOf(4 to 3), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_whenTargetMissing_recordsWarning() {
        val chronicle = GameChronicle()
        val player = playerWithDice(PLAYER_ID, TestDie(12, 5), TestDie(6, 3), TestDie(4, 1))
        val battle = setupBattle(
            player,
            playerWithDice(2, TestDie(10, 7), TestDie(6, 3), TestDie(4, 1)),
            playerWithDice(3, TestDie(8, 5), TestDie(6, 3), TestDie(4, 1)),
            playerWithDice(4, TestDie(6, 4), TestDie(8, 3), TestDie(4, 1))
        )

        FlipHigherOpposingDiceOnStrikeRowBattle(chronicle)(
            battle = battle,
            player = player,
            card = loadCard(),
            target = null,
            row = BattleStrikeRow.STRIKE_1
        )

        val warning = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.FLIP_TARGET_MISSING, warning.type)
    }

    @Test
    fun invoke_whenOwnDieNotFoundOnRow_recordsWarning() {
        val chronicle = GameChronicle()
        val player = playerWithDice(PLAYER_ID, TestDie(12, 5), TestDie(6, 3), TestDie(4, 1))
        val battle = setupBattle(
            player,
            playerWithDice(2, TestDie(10, 7), TestDie(6, 3), TestDie(4, 1)),
            playerWithDice(3, TestDie(8, 5), TestDie(6, 3), TestDie(4, 1)),
            playerWithDice(4, TestDie(6, 4), TestDie(8, 3), TestDie(4, 1))
        )

        FlipHigherOpposingDiceOnStrikeRowBattle(chronicle)(
            battle = battle,
            player = player,
            card = loadCard(),
            target = ExecuteTarget(dice = diceOf(TestDie(20, 5))),
            row = BattleStrikeRow.STRIKE_1
        )

        val warning = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.FLIP_DIE_NOT_FOUND, warning.type)
    }

    private fun setupBattle(vararg players: Player): Battle {
        return Battle().apply {
            setup(players.toList())
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
            .copy(effect = CardEffect.FLIP_HIGHER_OPPOSING_DICE_ON_STRIKE_ROW)
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
