package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.domain.CreatureCard
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckRefreshTest {

    private lateinit var card1: GameCard
    private lateinit var card2: GameCard
    private lateinit var SUT: CheckRefresh

    @BeforeEach
    fun setup() {
        val registry = GameCardRegistry()
        registry.loadFromCsv(Commons.CARD_LIST)
        card1 = requireNotNull(registry.getCard("Root_05_01"))
        card2 = requireNotNull(registry.getCard("Root_07_01"))
        SUT = CheckRefresh()
    }

    @Test
    fun invoke_whenCreatureHasNoCards_returnsFalse() {
        val player = Player()

        val result = SUT(player)

        assertFalse(result)
        assertEquals(emptyList(), player.creatureCards)
    }

    @Test
    fun invoke_whenAllCreatureCardsAreFaceDown_flipsAllFaceUpAndReturnsTrue() {
        val player = Player()
        player.addCardLeft(card1)
        player.addCardRight(card2)

        val result = SUT(player)

        assertTrue(result)
        assertTrue(player.creatureCards.all { it.isFaceUp })
    }

    @Test
    fun invoke_whenAnyCreatureCardIsFaceUp_returnsFalseAndLeavesCardsUnchanged() {
        val player = Player()
        player.addCardToCreature(CreatureCard(card1, CreatureCard.Facing.FACE_UP))
        player.addCardRight(card2)

        val result = SUT(player)

        assertFalse(result)
        assertEquals(listOf(true, false), player.creatureCards.map { it.isFaceUp })
    }

    @Test
    fun invoke_whenSingleCardIsFaceDown_flipsItFaceUp() {
        val player = Player()
        player.addCardLeft(card1)

        val result = SUT(player)

        assertTrue(result)
        assertTrue(player.getCreatureLeftCard(0)!!.isFaceUp)
    }
}
