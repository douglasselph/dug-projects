package dugsolutions.leaf.v35.player.dice

import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.SampleDie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerDiceTest {

    private lateinit var dice: SampleDie

    @BeforeEach
    fun setup() {
        dice = SampleDie(
            Randomizer.create(seed = 12345L)
        )
    }

    @Test
    fun newPlayerDice_hasEmptyZones() {
        val playerDice = PlayerDice()

        assertTrue(playerDice.supply.isEmpty())
        assertTrue(playerDice.hand.isEmpty())
        assertTrue(playerDice.discard.isEmpty())

        assertEquals(0, playerDice.supplySize)
        assertEquals(0, playerDice.handSize)
        assertEquals(0, playerDice.discardSize)

        assertTrue(playerDice.isSupplyEmpty)
        assertTrue(playerDice.isHandEmpty)
        assertTrue(playerDice.isDiscardEmpty)
    }

    @Test
    fun constructor_initializesAllThreeZones() {
        val supply = listOf(dice.d4, dice.d6)
        val hand = listOf(dice.d8)
        val discard = listOf(dice.d10, dice.d12)

        val playerDice = PlayerDice(
            supply = supply,
            hand = hand,
            discard = discard
        )

        assertEquals(
            supply.sortedBy { it.sides },
            playerDice.supply.sortedBy { it.sides }
        )
        assertEquals(hand, playerDice.hand)
        assertEquals(
            discard.sortedBy { it.sides },
            playerDice.discard.sortedBy { it.sides }
        )
    }

    @Test
    fun addToSupply_addsDieOnlyToSupplyAndReturnsSamePlayerDice() {
        val playerDice = PlayerDice()
        val die = dice.d6

        val result = playerDice.addToSupply(die)

        assertTrue(result === playerDice)
        assertEquals(listOf(die), playerDice.supply)
        assertTrue(playerDice.hand.isEmpty())
        assertTrue(playerDice.discard.isEmpty())
    }

    @Test
    fun addAllToSupply_addsAllDice() {
        val playerDice = PlayerDice()
        val incoming = listOf(
            dice.d8,
            dice.d4,
            dice.d6
        )

        val result = playerDice.addAllToSupply(incoming)

        assertTrue(result === playerDice)
        assertEquals(
            incoming.sortedBy { it.sides },
            playerDice.supply.sortedBy { it.sides }
        )
    }

    @Test
    fun addToHand_addsDieOnlyToHandAndReturnsSamePlayerDice() {
        val playerDice = PlayerDice()
        val die = dice.d8

        val result = playerDice.addToHand(die)

        assertTrue(result === playerDice)
        assertEquals(listOf(die), playerDice.hand)
        assertTrue(playerDice.supply.isEmpty())
        assertTrue(playerDice.discard.isEmpty())
    }

    @Test
    fun addToDiscard_addsDieOnlyToDiscardAndReturnsSamePlayerDice() {
        val playerDice = PlayerDice()
        val die = dice.d10

        val result = playerDice.addToDiscard(die)

        assertTrue(result === playerDice)
        assertEquals(listOf(die), playerDice.discard)
        assertTrue(playerDice.supply.isEmpty())
        assertTrue(playerDice.hand.isEmpty())
    }

    @Test
    fun addAllToDiscard_addsAllDice() {
        val playerDice = PlayerDice()
        val incoming = listOf(
            dice.d12,
            dice.d6,
            dice.d8
        )

        val result = playerDice.addAllToDiscard(incoming)

        assertTrue(result === playerDice)
        assertEquals(
            incoming.sortedBy { it.sides },
            playerDice.discard.sortedBy { it.sides }
        )
    }

    @Test
    fun draw_whenSupplyAndDiscardAreEmpty_returnsNull() {
        val playerDice = PlayerDice()

        val result = playerDice.draw()

        assertNull(result)
        assertTrue(playerDice.supply.isEmpty())
        assertTrue(playerDice.hand.isEmpty())
        assertTrue(playerDice.discard.isEmpty())
    }

    @Test
    fun draw_takesLowestSidedDieFromSupply() {
        val playerDice = PlayerDice()
        val d8 = dice.d8
        val d4 = dice.d4
        val d12 = dice.d12

        playerDice.addAllToSupply(
            listOf(d8, d4, d12)
        )

        val result = playerDice.draw()

        assertEquals(4, result!!.sides)
        assertEquals(listOf(result), playerDice.hand)
        assertEquals(
            listOf(d8, d12).sortedBy { it.sides },
            playerDice.supply.sortedBy { it.sides }
        )
        assertTrue(playerDice.discard.isEmpty())
    }

    @Test
    fun draw_rollsSelectedDieExactlyOnce() {
        val playerDice = PlayerDice()
        val die = TrackingDie(6)

        playerDice.addToSupply(die)

        val result = playerDice.draw()

        assertTrue(result === die)
        assertEquals(1, die.rollCount)
        assertEquals(listOf(die), playerDice.hand)
    }

    @Test
    fun draw_whenSupplyHasDice_doesNotRefillFromDiscard() {
        val supplyDie = TrackingDie(8)
        val discardDie = TrackingDie(4)

        val playerDice = PlayerDice(
            supply = listOf(supplyDie),
            discard = listOf(discardDie)
        )

        val result = playerDice.draw()

        assertTrue(result === supplyDie)
        assertEquals(listOf(supplyDie), playerDice.hand)
        assertTrue(playerDice.supply.isEmpty())
        assertEquals(listOf(discardDie), playerDice.discard)
        assertEquals(1, supplyDie.rollCount)
        assertEquals(0, discardDie.rollCount)
    }

    @Test
    fun draw_whenSupplyEmpty_refillsFromDiscardThenDrawsLowest() {
        val d8 = TrackingDie(8)
        val d4 = TrackingDie(4)
        val d6 = TrackingDie(6)

        val playerDice = PlayerDice(
            discard = listOf(d8, d4, d6)
        )

        val result = playerDice.draw()

        assertEquals(4, result!!.sides)
        assertEquals(listOf(result), playerDice.hand)
        assertEquals(
            listOf(d6, d8).sortedBy { it.sides },
            playerDice.supply.sortedBy { it.sides }
        )
        assertTrue(playerDice.discard.isEmpty())

        assertEquals(1, d4.rollCount)
        assertEquals(0, d6.rollCount)
        assertEquals(0, d8.rollCount)
    }

    @Test
    fun repeatedDraw_refillsOnlyWhenSupplyBecomesEmpty() {
        val d4 = TrackingDie(4)
        val d6 = TrackingDie(6)
        val d8 = TrackingDie(8)

        val playerDice = PlayerDice(
            supply = listOf(d4, d6),
            discard = listOf(d8)
        )

        val first = playerDice.draw()
        val second = playerDice.draw()

        assertEquals(4, first!!.sides)
        assertEquals(6, second!!.sides)
        assertTrue(playerDice.supply.isEmpty())
        assertEquals(listOf(d8), playerDice.discard)

        val third = playerDice.draw()

        assertEquals(8, third!!.sides)
        assertTrue(playerDice.supply.isEmpty())
        assertTrue(playerDice.discard.isEmpty())
        assertEquals(3, playerDice.handSize)
    }

    @Test
    fun removeFromHand_whenMatchingDieExists_removesOneAndReturnsDie() {
        val first = FixedDie(6, 4)
        val second = FixedDie(8, 3)

        val playerDice = PlayerDice(
            hand = listOf(first, second)
        )

        val result = playerDice.removeFromHand(first)

        assertEquals(first, result)
        assertEquals(listOf(second), playerDice.hand)
    }

    @Test
    fun removeFromHand_whenEquivalentDieExists_removesOneMatchingDie() {
        val stored = FixedDie(6, 4)

        val playerDice = PlayerDice(
            hand = listOf(stored)
        )

        val equivalent = FixedDie(6, 4)

        val result = playerDice.removeFromHand(equivalent)

        assertEquals(equivalent, result)
        assertTrue(playerDice.hand.isEmpty())
    }

    @Test
    fun removeFromHand_whenNoMatchingDieExists_returnsNull() {
        val playerDice = PlayerDice(
            hand = listOf(FixedDie(6, 4))
        )

        val result = playerDice.removeFromHand(
            FixedDie(8, 4)
        )

        assertNull(result)
        assertEquals(1, playerDice.handSize)
    }

    @Test
    fun removeFromDiscard_whenMatchingDieExists_removesOneAndReturnsDie() {
        val first = FixedDie(8, 3)
        val second = FixedDie(10, 7)

        val playerDice = PlayerDice(
            discard = listOf(first, second)
        )

        val result = playerDice.removeFromDiscard(first)

        assertEquals(first, result)
        assertEquals(listOf(second), playerDice.discard)
    }

    @Test
    fun removeFromDiscard_whenNoMatchingDieExists_returnsNull() {
        val playerDice = PlayerDice(
            discard = listOf(FixedDie(8, 3))
        )

        val result = playerDice.removeFromDiscard(
            FixedDie(12, 3)
        )

        assertNull(result)
        assertEquals(1, playerDice.discardSize)
    }

    @Test
    fun discardHand_movesAllHandDiceToDiscardAndEmptiesHand() {
        val handOne = FixedDie(4, 3)
        val handTwo = FixedDie(8, 7)
        val existingDiscard = FixedDie(6, 2)

        val playerDice = PlayerDice(
            hand = listOf(handOne, handTwo),
            discard = listOf(existingDiscard)
        )

        playerDice.discardHand()

        assertTrue(playerDice.hand.isEmpty())
        assertEquals(
            listOf(existingDiscard, handOne, handTwo)
                .sortedBy { it.sides },
            playerDice.discard.sortedBy { it.sides }
        )
    }

    @Test
    fun discardHand_preservesCurrentDieValues() {
        val first = FixedDie(6, 5)
        val second = FixedDie(12, 11)

        val playerDice = PlayerDice(
            hand = listOf(first, second)
        )

        playerDice.discardHand()

        assertEquals(
            listOf(5, 11),
            playerDice.discard
                .sortedBy { it.sides }
                .map { it.value }
        )
    }

    @Test
    fun discardHand_whenHandEmpty_doesNothing() {
        val existingDiscard = FixedDie(6, 3)

        val playerDice = PlayerDice(
            discard = listOf(existingDiscard)
        )

        playerDice.discardHand()

        assertTrue(playerDice.hand.isEmpty())
        assertEquals(
            listOf(existingDiscard),
            playerDice.discard
        )
    }

    @Test
    fun clear_removesDiceFromAllThreeZones() {
        val playerDice = PlayerDice(
            supply = listOf(dice.d4),
            hand = listOf(dice.d6),
            discard = listOf(dice.d8)
        )

        playerDice.clear()

        assertTrue(playerDice.supply.isEmpty())
        assertTrue(playerDice.hand.isEmpty())
        assertTrue(playerDice.discard.isEmpty())

        assertEquals(0, playerDice.supplySize)
        assertEquals(0, playerDice.handSize)
        assertEquals(0, playerDice.discardSize)
    }

    @Test
    fun zoneLists_areStructuralSnapshots() {
        val playerDice = PlayerDice()
        val first = dice.d4
        val second = dice.d6

        playerDice.addToSupply(first)
        val supplySnapshot = playerDice.supply

        playerDice.addToSupply(second)

        assertEquals(1, supplySnapshot.size)
        assertEquals(2, playerDice.supply.size)
    }

    @Test
    fun zoneLists_containLiveDiceObjects() {
        val die = FixedDie(8, 3)
        val playerDice = PlayerDice(
            hand = listOf(die)
        )

        die.adjustTo(7)

        assertEquals(7, playerDice.hand.single().value)
    }

    private class TrackingDie(
        sides: Int
    ) : Die(sides) {

        var rollCount = 0

        override fun roll(): Die {
            rollCount++
            return this
        }
    }

    private class FixedDie(
        sides: Int,
        value: Int
    ) : Die(sides) {

        init {
            adjustTo(value)
        }

        override fun roll(): Die =
            this
    }
    @Test
    fun removeExactFromHand_preservesDistinctEquivalentDiceIdentity() {
        val first = FixedDie(6, 4)
        val second = FixedDie(6, 4)
        val dice = PlayerDice(hand = listOf(first, second))

        val removed = dice.removeExactFromHand(second)

        assertTrue(removed === second)
        assertEquals(1, dice.handSize)
        assertTrue(dice.hand.single() === first)
    }

}
