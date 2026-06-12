package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RaiseDiePlus1AndDoubleMatchingDiceCultivationTest {

    @Test
    fun invoke_whenTwoDiceAlreadyMatch_doublesBothThenRaisesFirst() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val first = FixedDie(6, 2)
        val second = FixedDie(8, 2)
        val player = Player(id = 1).apply {
            addDieToHand(first)
            addDieToHand(second)
        }

        RaiseDiePlus1AndDoubleMatchingDiceCultivation(chronicle)(
            player = player,
            card = card,
            target = Dice(listOf(FixedDie(6, 2), FixedDie(8, 2)))
        )

        assertEquals(5, first.value)
        assertEquals(4, second.value)
        val entry = assertIs<GameEntry.GameCardEffect>(chronicle.getEntries().single())
        assertEquals(listOf(6 to 5, 8 to 4), entry.dice.map { it.sides to it.value })
    }

    @Test
    fun invoke_whenTwoDiceMatchAfterRaise_raisesFirstThenDoublesBoth() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val first = FixedDie(6, 2)
        val second = FixedDie(8, 3)
        val player = Player(id = 1).apply {
            addDieToHand(first)
            addDieToHand(second)
        }

        RaiseDiePlus1AndDoubleMatchingDiceCultivation(chronicle)(
            player = player,
            card = card,
            target = Dice(listOf(FixedDie(6, 2), FixedDie(8, 3)))
        )

        assertEquals(6, first.value)
        assertEquals(6, second.value)
    }

    @Test
    fun invoke_whenThreeDice_firstTwoDoubleAndThirdRaises() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val first = FixedDie(6, 2)
        val second = FixedDie(8, 2)
        val third = FixedDie(10, 5)
        val player = Player(id = 1).apply {
            addDieToHand(first)
            addDieToHand(second)
            addDieToHand(third)
        }

        RaiseDiePlus1AndDoubleMatchingDiceCultivation(chronicle)(
            player = player,
            card = card,
            target = Dice(listOf(FixedDie(6, 2), FixedDie(8, 2), FixedDie(10, 5)))
        )

        assertEquals(4, first.value)
        assertEquals(4, second.value)
        assertEquals(6, third.value)
    }

    @Test
    fun invoke_whenTargetIsInvalid_recordsWarning() {
        val chronicle = GameChronicle()
        val card = loadCard()
        val player = Player(id = 1).apply {
            addDieToHand(FixedDie(6, 2))
            addDieToHand(FixedDie(8, 5))
        }

        RaiseDiePlus1AndDoubleMatchingDiceCultivation(chronicle)(
            player = player,
            card = card,
            target = Dice(listOf(FixedDie(6, 2), FixedDie(8, 5)))
        )

        val entry = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.DOUBLE_MATCHING_DICE_INVALID_TARGET, entry.type)
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
}
