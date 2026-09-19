package dugsolutions.leaf.v30.table.di

import dugsolutions.leaf.v30.cards.GameCardManager
import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.di.GameCardsFactory
import dugsolutions.leaf.v30.cards.domain.GameCards
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.grove.domain.GroveCardStackID
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.table.domain.GameLength
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class TableConfigFactoryTest {

    private lateinit var cardManager: GameCardManager
    private lateinit var cardsFactory: GameCardsFactory
    private lateinit var SUT: TableConfigFactory

    @BeforeEach
    fun setup() {
        val registry = GameCardRegistry()
        registry.loadFromCsv(Commons.CARD_LIST)
        cardsFactory = GameCardsFactory()
        cardManager = GameCardManager(cardsFactory)
        cardManager.loadCards(registry)
        SUT = TableConfigFactory(cardManager, cardsFactory, IdentityRandomizer())
    }

    @Test
    fun invoke_withGameLength_returnsConfigUsingGameLengthCounts() {
        val cards = GameCards(emptyList())

        val result = SUT(cards, numPlayers = 2, gameLength = GameLength.SHORT)

        assertEquals(cards, result.cards)
        assertEquals(2, result.numPlayers)
        assertEquals(GameLength.SHORT.numBattle, result.numBattle)
        assertEquals(GameLength.SHORT.numCultivation, result.numCultivation)
    }

    @Test
    fun invoke_withExplicitCounts_returnsConfigUsingExplicitCounts() {
        val cards = GameCards(emptyList())

        val result = SUT(cards, numPlayers = 4, numBattle = 5, numCultivation = 6)

        assertEquals(cards, result.cards)
        assertEquals(4, result.numPlayers)
        assertEquals(5, result.numBattle)
        assertEquals(6, result.numCultivation)
    }

    @Test
    fun random_withGameLength_selectsOneCardForEachGroveStack() {
        val result = SUT.random(numPlayers = 3, gameLength = GameLength.LONG)

        assertEquals(3, result.numPlayers)
        assertEquals(GameLength.LONG.numBattle, result.numBattle)
        assertEquals(GameLength.LONG.numCultivation, result.numCultivation)
        assertEquals(GroveCardStackID.entries.size, result.cards.size)
        GroveCardStackID.entries.forEach { stackId ->
            assertEquals(
                1,
                result.cards.cards.count { card -> card.type == stackId.type && card.cost == stackId.cost },
                "Expected one selected card for $stackId"
            )
        }
    }

    @Test
    fun random_withExplicitCounts_selectsOneCardForEachGroveStackAndUsesExplicitCounts() {
        val result = SUT.random(numPlayers = 2, numBattle = 1, numCultivation = 2)

        assertEquals(2, result.numPlayers)
        assertEquals(1, result.numBattle)
        assertEquals(2, result.numCultivation)
        assertEquals(GroveCardStackID.entries.size, result.cards.size)
    }

    @Test
    fun random_whenCardsAreNotLoaded_throwsException() {
        val emptyManager = GameCardManager(cardsFactory)
        val factory = TableConfigFactory(emptyManager, cardsFactory, IdentityRandomizer())

        assertThrows<IllegalStateException> {
            factory.random(numPlayers = 2, gameLength = GameLength.SHORT)
        }
    }

    private class IdentityRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = throw UnsupportedOperationException()
        override fun nextInt(from: Int, until: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = throw UnsupportedOperationException()
        override fun <T> randomOrNull(list: List<T>): T? = throw UnsupportedOperationException()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
