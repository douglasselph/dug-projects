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

class DrainHigherDiceAndRaiseOwnDieBattleTest {
    private companion object {
        const val PLAYER_ID = 1
    }

    @Test
    fun invoke_drainsHigherOpponentDiceAndRaisesOwnDieByTwoPerAffectedDie() {
        val chronicle = GameChronicle()
        val ownDie = TestDie(12, 5)
        val higherOpponent1 = TestDie(10, 7)
        val higherOpponent2 = TestDie(8, 6)
        val equalOpponent = TestDie(6, 5)
        val lowerOpponent = TestDie(4, 2)
        val player = playerWithDice(PLAYER_ID, ownDie, TestDie(6, 3), TestDie(4, 1))
        val opponent2 = playerWithDice(2, higherOpponent1, TestDie(6, 3), TestDie(4, 1))
        val opponent3 = playerWithDice(3, higherOpponent2, TestDie(6, 3), TestDie(4, 1))
        val opponent4 = playerWithDice(4, equalOpponent, lowerOpponent, TestDie(4, 1))
        val battle = setupBattle(player, opponent2, opponent3, opponent4)

        DrainHigherDiceAndRaiseOwnDieBattle(chronicle)(
            battle = battle,
            player = player,
            card = loadCard(),
            target = ExecuteTarget(dice = diceOf(TestDie(12, 5))),
            row = BattleStrikeRow.STRIKE_1
        )

        assertEquals(9, ownDie.value)
        assertEquals(5, higherOpponent1.value)
        assertEquals(4, higherOpponent2.value)
        assertEquals(5, equalOpponent.value)
        assertEquals(2, lowerOpponent.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.DRAIN_HIGHER_DICE_AND_RAISE_OWN_DIE, entry.effect)
        assertEquals("Drained 2 higher opposing die/dice by 2 and raised own die by 4", entry.detail)
        assertEquals(listOf(8 to 4, 10 to 5, 12 to 9), entry.dice.map { it.sides to it.value }.sortedBy { it.first })
    }

    @Test
    fun invoke_whenDrainedDieWouldGoBelowOne_clampsAtOne() {
        val chronicle = GameChronicle()
        val ownDie = TestDie(4, 1)
        val higherOpponent = TestDie(6, 2)
        val player = playerWithDice(PLAYER_ID, ownDie, TestDie(6, 1), TestDie(8, 1))
        val opponent2 = playerWithDice(2, higherOpponent, TestDie(8, 1), TestDie(10, 1))
        val opponent3 = playerWithDice(3, TestDie(8, 1), TestDie(6, 1), TestDie(4, 1))
        val opponent4 = playerWithDice(4, TestDie(10, 1), TestDie(6, 1), TestDie(4, 1))
        val battle = setupBattle(player, opponent2, opponent3, opponent4)

        DrainHigherDiceAndRaiseOwnDieBattle(chronicle)(
            battle = battle,
            player = player,
            card = loadCard(),
            target = ExecuteTarget(dice = diceOf(TestDie(4, 1))),
            row = BattleStrikeRow.STRIKE_1
        )

        assertEquals(3, ownDie.value)
        assertEquals(1, higherOpponent.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(listOf(4 to 3, 6 to 1), entry.dice.map { it.sides to it.value }.sortedBy { it.first })
    }

    @Test
    fun invoke_whenTargetMissing_recordsWarning() {
        val chronicle = GameChronicle()
        val player = playerWithDice(PLAYER_ID, TestDie(12, 5), TestDie(6, 3), TestDie(4, 1))
        val battle = setupBattle(
            player,
            playerWithDice(2, TestDie(10, 7), TestDie(6, 3), TestDie(4, 1)),
            playerWithDice(3, TestDie(8, 6), TestDie(6, 3), TestDie(4, 1)),
            playerWithDice(4, TestDie(4, 2), TestDie(6, 3), TestDie(4, 1))
        )

        DrainHigherDiceAndRaiseOwnDieBattle(chronicle)(
            battle = battle,
            player = player,
            card = loadCard(),
            target = null,
            row = BattleStrikeRow.STRIKE_1
        )

        val warning = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.RAISE_TARGET_MISSING, warning.type)
    }

    @Test
    fun invoke_whenOwnDieNotFoundOnRow_recordsWarning() {
        val chronicle = GameChronicle()
        val player = playerWithDice(PLAYER_ID, TestDie(12, 5), TestDie(6, 3), TestDie(4, 1))
        val battle = setupBattle(
            player,
            playerWithDice(2, TestDie(10, 7), TestDie(6, 3), TestDie(4, 1)),
            playerWithDice(3, TestDie(8, 6), TestDie(6, 3), TestDie(4, 1)),
            playerWithDice(4, TestDie(4, 2), TestDie(6, 3), TestDie(4, 1))
        )

        DrainHigherDiceAndRaiseOwnDieBattle(chronicle)(
            battle = battle,
            player = player,
            card = loadCard(),
            target = ExecuteTarget(dice = diceOf(TestDie(20, 5))),
            row = BattleStrikeRow.STRIKE_1
        )

        val warning = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.RAISE_DIE_NOT_FOUND, warning.type)
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
            .copy(effect = CardEffect.DRAIN_HIGHER_DICE_AND_RAISE_OWN_DIE)
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
