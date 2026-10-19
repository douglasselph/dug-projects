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
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RaiseDiePlus1AndDoubleMatchingDiceBattleTest {

    @Test
    fun invoke_whenTwoDiceAlreadyMatch_findsDiceFromStartingRowAndMutatesGrid() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val first = FixedDie(6, 2)
        val second = FixedDie(8, 2)
        val target = player(1, first, second, FixedDie(10, 1))
        val battle = setupBattle(target)

        RaiseDiePlus1AndDoubleMatchingDiceBattle(chronicle)(
            battle = battle,
            player = Player(id = 9),
            targetPlayer = target,
            row = BattleStrikeRow.STRIKE_1,
            card = card,
            target = Dice(listOf(FixedDie(6, 2), FixedDie(8, 2)))
        )

        assertEquals(5, first.value)
        assertEquals(4, second.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(listOf(6 to 5, 8 to 4), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_whenTargetIsInvalid_recordsWarning() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val target = player(1, FixedDie(6, 2), FixedDie(8, 5), FixedDie(10, 1))
        val battle = setupBattle(target)

        RaiseDiePlus1AndDoubleMatchingDiceBattle(chronicle)(
            battle = battle,
            player = Player(id = 9),
            targetPlayer = target,
            row = BattleStrikeRow.STRIKE_1,
            card = card,
            target = Dice(listOf(FixedDie(6, 2), FixedDie(8, 5)))
        )

        val entry = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.DOUBLE_MATCHING_DICE_INVALID_TARGET, entry.type)
    }

    private fun setupBattle(target: Player): Battle {
        return Battle(playerGridOrder = PlayerGridOrder(SequentialRandomizer())).apply {
            setup(
                listOf(
                    target,
                    player(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                    player(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                    player(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
                )
            )
        }
    }

    private fun player(
        id: Int,
        vararg dice: Die
    ): Player {
        return Player(id = id).apply {
            dice.forEach { addDieToSupply(it) }
            repeat(dice.size) { drawDie() }
        }
    }

    private fun loadCard(): GameCard {
        return GameCardRegistry()
            .apply { loadFromCsv(Commons.CARD_LIST) }
            .getAllCards()
            .first()
            .copy(effect = CardEffect.RAISE_DIE_PLUS_1_AND_DOUBLE_MATCHING_DICE)
    }

    private class FixedDie(
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
