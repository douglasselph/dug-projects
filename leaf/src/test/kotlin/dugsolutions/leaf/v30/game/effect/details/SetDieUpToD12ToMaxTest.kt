package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
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

class SetDieUpToD12ToMaxTest {

    @Test
    fun invoke_withHandScope_setsTargetDieToMax() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val die = TestDie(12, 4)
        val player = Player(id = 1).apply {
            addDieToHand(die)
        }

        SetDieUpToD12ToMax(chronicle)(
            scope = HandleDieEffectScope(player),
            card = card,
            target = ExecuteTarget(dice = diceOf(TestDie(12, 4)))
        )

        assertEquals(12, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.SET_DIE_UP_TO_D12_TO_MAX, entry.effect)
        assertEquals(listOf(12 to 12), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_withBattleScope_setsTargetBattleDieToMax() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val die = TestDie(10, 3)
        val player = playerWithDice(1, die, TestDie(8, 2), TestDie(6, 1))
        val battle = setupBattle(player)

        SetDieUpToD12ToMax(chronicle)(
            scope = BattleDieEffectScope(
                battle = battle,
                actingPlayer = player,
                targetPlayer = player,
                row = BattleStrikeRow.STRIKE_1
            ),
            card = card,
            target = ExecuteTarget(dice = diceOf(TestDie(10, 3)))
        )

        assertEquals(10, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.SET_DIE_UP_TO_D12_TO_MAX, entry.effect)
        assertEquals(listOf(10 to 10), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_withD20_ignoresDieAndChroniclesNoChange() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val die = TestDie(20, 5)
        val player = Player(id = 1).apply {
            addDieToHand(die)
        }

        SetDieUpToD12ToMax(chronicle)(
            scope = HandleDieEffectScope(player),
            card = card,
            target = ExecuteTarget(dice = diceOf(TestDie(20, 5)))
        )

        assertEquals(5, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(CardEffect.SET_DIE_UP_TO_D12_TO_MAX, entry.effect)
        assertEquals("Ignored D20; only dice up to D12 can be set to max", entry.detail)
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
            .copy(effect = CardEffect.SET_DIE_UP_TO_D12_TO_MAX)
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
