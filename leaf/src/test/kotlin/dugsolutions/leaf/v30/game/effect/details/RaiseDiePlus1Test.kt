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

class RaiseDiePlus1Test {
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

        RaiseDiePlus1(chronicle)(
            scope = HandleDieEffectScope(player),
            card = card,
            target = ExecuteTarget(dice = diceOf(TestDie(8, 3)))
        )

        assertEquals(4, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.RAISE_DIE_PLUS_1_AND_END_GAME_PLUS_2_VP, entry.effect)
        assertEquals("Raised one die in player 1's hand by 1", entry.detail)
        assertEquals(listOf(8 to 4), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_withBattleScope_raisesTargetBattleDieByOne() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val die = TestDie(10, 4)
        val player = playerWithDice(PLAYER_ID, die, TestDie(8, 3), TestDie(6, 1))
        val battle = setupBattle(player)

        RaiseDiePlus1(chronicle)(
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
        assertEquals("Raised one die in player 1's STRIKE_1 battle square by 1", entry.detail)
        assertEquals(listOf(10 to 5), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_whenTargetMissing_recordsWarning() {
        val chronicle = GameChronicle()
        val player = Player(id = PLAYER_ID)

        RaiseDiePlus1(chronicle)(
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

        RaiseDiePlus1(chronicle)(
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
