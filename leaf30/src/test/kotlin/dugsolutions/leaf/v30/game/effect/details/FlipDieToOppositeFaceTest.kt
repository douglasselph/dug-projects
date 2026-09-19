package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.PlayerGridOrder
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
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FlipDieToOppositeFaceTest {

    @Test
    fun invoke_withHandScope_flipsTargetHandDie() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val die = TestDie(8, 3)
        val player = Player(id = 1).apply { addDieToHand(die) }

        FlipDieToOppositeFace(chronicle)(
            scope = HandleDieEffectScope(player),
            card = card,
            target = ExecuteTarget(player = player, dice = diceOf(TestDie(8, 3)))
        )

        assertEquals(6, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(card.effect, entry.effect)
        assertEquals("Flipped one die in player 1's hand to its opposite face", entry.detail)
        assertEquals(listOf(8 to 6), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_withBattleScope_flipsTargetBattleGridDie() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val targetDie = TestDie(8, 6)
        val target = player(1, targetDie, TestDie(6, 3), TestDie(4, 1))
        val acting = Player(id = 9)
        val battle = setupBattle(target)

        FlipDieToOppositeFace(chronicle)(
            scope = BattleDieEffectScope(
                battle = battle,
                actingPlayer = acting,
                targetPlayer = target,
                row = BattleStrikeRow.STRIKE_1
            ),
            card = card,
            target = ExecuteTarget(player = target, dice = diceOf(TestDie(8, 6)))
        )

        assertEquals(3, targetDie.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(card.effect, entry.effect)
        assertEquals("Flipped one die in player 1's STRIKE_1 battle square to its opposite face", entry.detail)
        assertEquals(listOf(8 to 3), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_withD4_doesNotChangeValue() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val die = TestDie(4, 3)
        val player = Player(id = 1).apply { addDieToHand(die) }

        FlipDieToOppositeFace(chronicle)(
            scope = HandleDieEffectScope(player),
            card = card,
            target = ExecuteTarget(player = player, dice = diceOf(TestDie(4, 3)))
        )

        assertEquals(3, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(listOf(4 to 3), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_whenTargetIsMissing_recordsWarning() {
        val chronicle = GameChronicle()
        val player = Player(id = 1).apply { addDieToHand(TestDie(8, 3)) }

        FlipDieToOppositeFace(chronicle)(
            scope = HandleDieEffectScope(player),
            card = loadCard(),
            target = null
        )

        val warning = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.FLIP_TARGET_MISSING, warning.type)
        assertEquals(1, warning.playerId)
    }

    @Test
    fun invoke_whenDieIsNotInScope_recordsWarning() {
        val chronicle = GameChronicle()
        val player = Player(id = 1).apply { addDieToHand(TestDie(8, 3)) }

        FlipDieToOppositeFace(chronicle)(
            scope = HandleDieEffectScope(player),
            card = loadCard(),
            target = ExecuteTarget(player = player, dice = diceOf(TestDie(6, 3)))
        )

        val warning = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.FLIP_DIE_NOT_FOUND, warning.type)
        assertEquals(1, warning.playerId)
    }

    private fun setupBattle(target: Player): Battle {
        return Battle(playerGridOrder = PlayerGridOrder(SequentialRandomizer())).apply {
            setup(
                listOf(
                    target,
                    player(2, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1)),
                    player(3, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1)),
                    player(4, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1))
                )
            )
        }
    }

    private fun player(
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
            .copy(effect = CardEffect.FLIP_DIE_TO_OPPOSITE_FACE)
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

    private class SequentialRandomizer : Randomizer {
        private var next = 1

        override fun nextBoolean(): Boolean = true

        override fun nextInt(from: Int, until: Int): Int {
            val result = next.coerceIn(from, until - 1)
            next++
            return result
        }

        override fun nextInt(until: Int): Int = nextInt(0, until)
        override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
