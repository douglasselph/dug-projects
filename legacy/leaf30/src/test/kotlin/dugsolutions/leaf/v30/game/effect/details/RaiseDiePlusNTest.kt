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
import dugsolutions.leaf.v30.game.effect.scope.BattleDieEffectScope
import dugsolutions.leaf.v30.game.effect.scope.HandleDieEffectScope
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RaiseDiePlusNTest {
    private companion object {
        const val PLAYER_ID = 1
    }

    @Test
    fun invoke_withHandScope_raisesTargetDieByOne() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val die = TestDie(8, 3)
        val player = Player(id = PLAYER_ID).apply {
            addDieToHand(die)
        }

        RaiseDiePlusN(chronicle)(
            scope = HandleDieEffectScope(player),
            card = card,
            target = ExecuteTarget(dice = diceOf(TestDie(8, 3)))
        )

        assertEquals(4, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.RAISE_DIE_PLUS_1_AND_END_GAME_PLUS_2_VP, entry.effect)
        assertEquals("Raised 1 dice in player 1's hand by 1", entry.detail)
        assertEquals(listOf(8 to 4), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_withHandScopeAndAmountFour_raisesTargetDieByFour() {
        val chronicle = GameChronicle()
        val card = loadCard().copy(effect = CardEffect.RAISE_DIE_PLUS_4)
        val die = TestDie(10, 3)
        val player = Player(id = PLAYER_ID).apply {
            addDieToHand(die)
        }

        RaiseDiePlusN(
            chronicle = chronicle,
            amount = 4
        )(
            scope = HandleDieEffectScope(player),
            card = card,
            target = ExecuteTarget(dice = diceOf(TestDie(10, 3)))
        )

        assertEquals(7, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.RAISE_DIE_PLUS_4, entry.effect)
        assertEquals("Raised 1 dice in player 1's hand by 4", entry.detail)
        assertEquals(listOf(10 to 7), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_withHandScope_raisesAllTargetDiceByOne() {
        val chronicle = GameChronicle()
        val card = loadCard().copy(effect = CardEffect.RAISE_THREE_DICE_PLUS_1)
        val d4 = TestDie(4, 1)
        val d6 = TestDie(6, 2)
        val d8 = TestDie(8, 3)
        val player = Player(id = PLAYER_ID).apply {
            addDieToHand(d4)
            addDieToHand(d6)
            addDieToHand(d8)
        }

        RaiseDiePlusN(chronicle)(
            scope = HandleDieEffectScope(player),
            card = card,
            target = ExecuteTarget(dice = diceOf(TestDie(4, 1), TestDie(6, 2), TestDie(8, 3)))
        )

        assertEquals(listOf(2, 3, 4), listOf(d4.value, d6.value, d8.value))
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.RAISE_THREE_DICE_PLUS_1, entry.effect)
        assertEquals(listOf(4 to 2, 6 to 3, 8 to 4), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_withBattleScope_raisesTargetBattleDieByOne() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val die = TestDie(10, 4)
        val player = playerWithDice(PLAYER_ID, die, TestDie(8, 3), TestDie(6, 1))
        val battle = setupBattle(player)

        RaiseDiePlusN(chronicle)(
            scope = BattleDieEffectScope(
                battle = battle,
                actingPlayer = player,
                targetPlayer = player,
                row = BattleStrikeRow.STRIKE_1
            ),
            card = card,
            target = ExecuteTarget(dice = diceOf(TestDie(10, 4)))
        )

        assertEquals(5, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals("Raised 1 dice in player 1's STRIKE_1 battle square by 1", entry.detail)
        assertEquals(listOf(10 to 5), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_withBattleScope_raisesEachTargetDieOnCorrespondingRow() {
        val chronicle = GameChronicle()
        val card = loadCard().copy(effect = CardEffect.RAISE_THREE_DICE_PLUS_1)
        val row1Die = TestDie(10, 4)
        val row2Die = TestDie(8, 3)
        val row3Die = TestDie(6, 2)
        val player = playerWithDice(PLAYER_ID, row1Die, row2Die, row3Die)
        val battle = setupBattle(player)

        RaiseDiePlusN(chronicle)(
            scope = BattleDieEffectScope(
                battle = battle,
                actingPlayer = player,
                targetPlayer = player,
                rows = listOf(BattleStrikeRow.STRIKE_1, BattleStrikeRow.STRIKE_2, BattleStrikeRow.STRIKE_3)
            ),
            card = card,
            target = ExecuteTarget(dice = diceOf(TestDie(10, 4), TestDie(8, 3), TestDie(6, 2)))
        )

        assertEquals(listOf(5, 4, 3), listOf(row1Die.value, row2Die.value, row3Die.value))
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.RAISE_THREE_DICE_PLUS_1, entry.effect)
        assertEquals(listOf(6 to 3, 8 to 4, 10 to 5), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_whenTargetMissing_recordsWarning() {
        val chronicle = GameChronicle()
        val player = Player(id = PLAYER_ID)

        RaiseDiePlusN(chronicle)(
            scope = HandleDieEffectScope(player),
            card = loadCard(),
            target = null
        )

        val warning = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.RAISE_TARGET_MISSING, warning.type)
        assertEquals(PLAYER_ID, warning.playerId)
    }

    @Test
    fun invoke_whenDieNotFound_recordsWarning() {
        val chronicle = GameChronicle()
        val player = Player(id = PLAYER_ID).apply {
            addDieToHand(TestDie(8, 3))
        }

        RaiseDiePlusN(chronicle)(
            scope = HandleDieEffectScope(player),
            card = loadCard(),
            target = ExecuteTarget(dice = diceOf(TestDie(6, 3)))
        )

        val warning = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.RAISE_DIE_NOT_FOUND, warning.type)
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
            .copy(effect = CardEffect.RAISE_DIE_PLUS_1_AND_END_GAME_PLUS_2_VP)
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
