package dugsolutions.leaf.v30.grove

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.cards.domain.GameCards
import dugsolutions.leaf.v30.common.Butterfly
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.grove.domain.GroveCardStackID
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.DieSides
import dugsolutions.leaf.v30.wisp.WispCardManager
import dugsolutions.leaf.v30.wisp.WispCardRegistry
import dugsolutions.leaf.v30.wisp.WispDeck
import dugsolutions.leaf.v30.wisp.di.WispCardsFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GroveTest {

    private lateinit var rootFiveOne: GameCard
    private lateinit var rootFiveTwo: GameCard
    private lateinit var SUT: Grove

    @BeforeEach
    fun setup() {
        val registry = GameCardRegistry()
        registry.loadFromCsv(Commons.CARD_LIST)
        rootFiveOne = requireNotNull(registry.getCard("Root_05_01"))
        rootFiveTwo = requireNotNull(registry.getCard("Root_05_02"))
        SUT = Grove(createWispDeck())
    }

    @Test
    fun constructor_resetsCommonSupplies() {
        Critter.entries.forEach { critter ->
            assertEquals(9, SUT.count(critter))
            assertTrue(SUT.has(critter))
        }
        assertEquals(8, SUT.count(Token.WATER))
        DieSides.entries.forEach { sides ->
            assertEquals(8, SUT.count(Token.MULCH(sides)))
        }
        Butterfly.entries.forEach { butterfly ->
            assertTrue(SUT.has(butterfly))
        }
        assertEquals(Butterfly.entries.size, SUT.butterflies.size)
    }

    @Test
    fun reset_restoresCrittersTokensButterfliesAndVp() {
        SUT.remove(Critter.BEE)
        SUT.remove(Token.WATER)
        SUT.remove(Token.MULCH(DieSides.D6))
        SUT.remove(Butterfly.RED)

        SUT.reset()

        assertEquals(9, SUT.count(Critter.BEE))
        assertEquals(9, SUT.count(Critter.WORM))
        assertEquals(8, SUT.count(Token.WATER))
        assertEquals(8, SUT.count(Token.MULCH(DieSides.D6)))
        Butterfly.entries.forEach { butterfly ->
            assertTrue(SUT.has(butterfly))
        }
    }

    @Test
    fun resetDice_withTwoPlayers_setsAllNonD4StacksToSeven() {
        SUT.resetDice(numPlayers = 2)

        assertEquals(0, SUT.diceStacks.getCount(DieSides.D4))
        DieSides.entries.filterNot { it == DieSides.D4 }.forEach { sides ->
            assertEquals(7, SUT.diceStacks.getCount(sides))
        }
    }

    @Test
    fun resetDice_withThreePlayers_setsAllNonD4StacksToEight() {
        SUT.resetDice(numPlayers = 3)

        assertEquals(0, SUT.diceStacks.getCount(DieSides.D4))
        DieSides.entries.filterNot { it == DieSides.D4 }.forEach { sides ->
            assertEquals(8, SUT.diceStacks.getCount(sides))
        }
    }

    @Test
    fun resetDice_withFourPlayers_setsAllNonD4StacksToNine() {
        SUT.resetDice(numPlayers = 4)

        assertEquals(0, SUT.diceStacks.getCount(DieSides.D4))
        DieSides.entries.filterNot { it == DieSides.D4 }.forEach { sides ->
            assertEquals(9, SUT.diceStacks.getCount(sides))
        }
    }

    @Test
    fun resetDice_withUnsupportedPlayerCount_throwsException() {
        assertThrows<IllegalArgumentException> {
            SUT.resetDice(numPlayers = 5)
        }
    }

    @Test
    fun setCard_setsMatchingCardStackToEightCards() {
        SUT.setCard(rootFiveOne)

        assertSame(rootFiveOne, SUT.cardStacks.getCard(GroveCardStackID.ROOT_5))
        assertEquals(8, SUT.cardStacks.getCount(GroveCardStackID.ROOT_5))
    }

    @Test
    fun setCard_replacesExistingCardInSameStack() {
        SUT.setCard(rootFiveOne)
        SUT.cardStacks.remove(rootFiveOne, amount = 3)

        SUT.setCard(rootFiveTwo)

        assertSame(rootFiveTwo, SUT.cardStacks.getCard(GroveCardStackID.ROOT_5))
        assertEquals(8, SUT.cardStacks.getCount(GroveCardStackID.ROOT_5))
    }

    @Test
    fun setCards_setsEachMatchingCardStackToEightCards() {
        SUT.setCards(GameCards(listOf(rootFiveOne, rootFiveTwo)))

        assertSame(rootFiveTwo, SUT.cardStacks.getCard(GroveCardStackID.ROOT_5))
        assertEquals(8, SUT.cardStacks.getCount(GroveCardStackID.ROOT_5))
    }

    @Test
    fun addAndRemoveCritter_updatesCritterSupply() {
        SUT.remove(Critter.BEE)
        SUT.add(Critter.BEE)

        assertEquals(9, SUT.count(Critter.BEE))
        assertTrue(SUT.remove(Critter.BEE))
        assertEquals(8, SUT.count(Critter.BEE))
    }

    @Test
    fun removeCritter_whenNoneAvailable_returnsFalse() {
        repeat(9) {
            assertTrue(SUT.remove(Critter.WORM))
        }

        assertFalse(SUT.remove(Critter.WORM))
        assertFalse(SUT.has(Critter.WORM))
    }

    @Test
    fun addAndRemoveToken_updatesTokenSupply() {
        val token = Token.MULCH(DieSides.D8)

        assertEquals(token, SUT.remove(token))
        assertEquals(7, SUT.count(token))
        SUT.add(token)

        assertEquals(8, SUT.count(token))
    }

    @Test
    fun removeToken_whenNoneAvailable_returnsNull() {
        repeat(8) {
            assertEquals(Token.WATER, SUT.remove(Token.WATER))
        }

        assertNull(SUT.remove(Token.WATER))
        assertFalse(SUT.has(Token.WATER))
    }

    @Test
    fun addAndRemoveButterfly_enforcesSingleButterflyOfEachType() {
        assertFalse(SUT.add(Butterfly.GREEN))
        assertTrue(SUT.remove(Butterfly.GREEN))
        assertFalse(SUT.has(Butterfly.GREEN))

        assertTrue(SUT.add(Butterfly.GREEN))
        assertTrue(SUT.has(Butterfly.GREEN))
        assertFalse(SUT.add(Butterfly.GREEN))
        assertEquals(Butterfly.values().size, SUT.butterflies.size)
    }

    @Test
    fun removeButterfly_whenMissing_returnsFalse() {
        assertTrue(SUT.remove(Butterfly.PURPLE))
        assertFalse(SUT.remove(Butterfly.PURPLE))
    }

    @Test
    fun resetWispDeck_restoresWispDeck() {
        val startingCount = SUT.wispDeck.remaining
        SUT.drawWispCard()
        SUT.drawWispCard()

        SUT.resetWispDeck()

        assertEquals(startingCount, SUT.wispDeck.remaining)
    }

    @Test
    fun drawWispCard_drawsFromWispDeck() {
        val startingCount = SUT.wispDeck.remaining

        val result = SUT.drawWispCard()

        assertTrue(result != null)
        assertEquals(startingCount - 1, SUT.wispDeck.remaining)
    }

    private fun createWispDeck(): WispDeck {
        val registry = WispCardRegistry()
        registry.loadFromCsv(Commons.WISP_LIST)
        val manager = WispCardManager(WispCardsFactory())
        manager.loadCards(registry)
        return WispDeck(manager, IdentityRandomizer())
    }

    private class IdentityRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }

}
