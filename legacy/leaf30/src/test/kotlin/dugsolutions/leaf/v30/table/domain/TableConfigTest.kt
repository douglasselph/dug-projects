package dugsolutions.leaf.v30.table.domain

import dugsolutions.leaf.v30.cards.domain.GameCards
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TableConfigTest {

    @Test
    fun constructor_withGameLength_usesGameLengthCounts() {
        val cards = GameCards(emptyList())

        val config = TableConfig(
            cards = cards,
            numPlayers = 3,
            gameLength = GameLength.SHORT
        )

        assertEquals(cards, config.cards)
        assertEquals(3, config.numPlayers)
        assertEquals(GameLength.SHORT.numBattle, config.numBattle)
        assertEquals(GameLength.SHORT.numCultivation, config.numCultivation)
    }

    @Test
    fun gameLength_longUsesFullRoundCardCounts() {
        assertEquals(6, GameLength.LONG.numBattle)
        assertEquals(6, GameLength.LONG.numCultivation)
    }
}
