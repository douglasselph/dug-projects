package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.round.RoundCardManager
import dugsolutions.leaf.v30.round.RoundDeck
import dugsolutions.leaf.v30.round.di.RoundCardsFactory
import dugsolutions.leaf.v30.table.Table
import dugsolutions.leaf.v30.wisp.WispCardManager
import dugsolutions.leaf.v30.wisp.WispDeck
import dugsolutions.leaf.v30.wisp.di.WispCardsFactory
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SimpleDieEffectsCultivationTest {

    @Test
    fun rerollDieUntilThreeOrHigher_rerollsHandDieUntilAtLeastThree() {
        val chronicle = GameChronicle()
        val card = loadCard(CardEffect.REROLL_DIE_UNTIL_THREE_OR_HIGHER)
        val die = SequenceDie(6, initial = 1, rolls = listOf(1, 2, 3))
        val player = Player(id = 1).apply { addDieToHand(die) }

        RerollDieUntilThreeOrHigherCultivation(chronicle)(
            player = player,
            card = card,
            target = ExecuteTarget.PlayerDie(player, diceOf(FixedDie(6, 1)))
        )

        assertEquals(3, die.rollCount)
        assertEquals(3, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(card.effect, entry.effect)
        assertEquals(listOf(6 to 3), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun raiseDiePlus1AndGainWater_raisesHandDieAndMovesWaterTokenToPlayer() {
        val chronicle = GameChronicle()
        val table = createTable()
        val card = loadCard(CardEffect.RAISE_DIE_PLUS_1_AND_GAIN_WATER)
        val die = FixedDie(6, 5)
        val player = Player(id = 1).apply { addDieToHand(die) }

        RaiseDiePlus1AndGainWaterCultivation(chronicle)(
            table = table,
            player = player,
            card = card,
            target = ExecuteTarget.PlayerDie(player, diceOf(FixedDie(6, 5)))
        )

        assertEquals(6, die.value)
        assertEquals(1, player.waterTokenCount)
        assertEquals(7, table.grove.count(Token.WATER))
        val entries = chronicle.getEntries().filterIsInstance<GameEntry.GameCardEffect>()
        assertEquals(listOf(6 to 6), entries[0].dice.map { it.sides to it.value })
        assertEquals(Token.WATER, entries[1].token)
    }

    @Test
    fun doubleOneDie_doublesTargetHandDie() {
        val chronicle = GameChronicle()
        val card = loadCard(CardEffect.DOUBLE_ONE_DIE)
        val die = FixedDie(8, 3)
        val player = Player(id = 1).apply { addDieToHand(die) }

        DoubleOneDieCultivation(chronicle)(
            player = player,
            card = card,
            target = ExecuteTarget.PlayerDie(player, diceOf(FixedDie(8, 3)))
        )

        assertEquals(6, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(listOf(8 to 6), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun flipDieToOppositeFace_flipsTargetHandDie() {
        val chronicle = GameChronicle()
        val card = loadCard(CardEffect.FLIP_DIE_TO_OPPOSITE_FACE)
        val die = FixedDie(8, 3)
        val player = Player(id = 1).apply { addDieToHand(die) }

        FlipDieToOppositeFaceCultivation(chronicle)(
            player = player,
            card = card,
            target = ExecuteTarget.PlayerDie(player, diceOf(FixedDie(8, 3)))
        )

        assertEquals(6, die.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(listOf(8 to 6), entry.dice.map { it.sides to it.value })
    }

    private fun createTable(): Table {
        val wispManager = WispCardManager(WispCardsFactory()).apply { loadCards(emptyList()) }
        val roundManager = RoundCardManager(RoundCardsFactory()).apply { loadCards(emptyList()) }
        return Table(
            grove = Grove(WispDeck(wispManager, IdentityRandomizer())),
            roundDeck = RoundDeck(roundManager, IdentityRandomizer())
        )
    }

    private fun loadCard(effect: CardEffect): GameCard {
        return GameCardRegistry()
            .apply { loadFromCsv(Commons.CARD_LIST) }
            .getAllCards()
            .first()
            .copy(effect = effect)
    }

    private fun diceOf(vararg dice: Die): Dice = Dice(dice.toList())

    private class FixedDie(
        sides: Int,
        value: Int
    ) : Die(sides) {
        init { adjustTo(value) }
        override fun roll(): Die = this
    }

    private class SequenceDie(
        sides: Int,
        initial: Int,
        private val rolls: List<Int>
    ) : Die(sides) {
        var rollCount = 0

        init { adjustTo(initial) }

        override fun roll(): Die {
            adjustTo(rolls.getOrElse(rollCount) { rolls.last() })
            rollCount++
            return this
        }
    }

    private class IdentityRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = true
        override fun nextInt(from: Int, until: Int): Int = from
        override fun nextInt(until: Int): Int = 0
        override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
