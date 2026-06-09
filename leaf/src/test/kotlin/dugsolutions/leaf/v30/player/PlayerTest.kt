package dugsolutions.leaf.v30.player

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.player.components.CreatureCard
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.SampleDie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerTest {

    private lateinit var cards: List<GameCard>
    private lateinit var dice: SampleDie
    private lateinit var player: Player

    @BeforeEach
    fun setup() {
        val registry = GameCardRegistry()
        registry.loadFromCsv(Commons.CARD_LIST)
        cards = registry.getAllCards()
        dice = SampleDie(Randomizer.create(seed = 12345L))
        player = Player()
    }

    @Test
    fun creature_whenNew_hasEmptyStacks() {
        assertTrue(player.creature.left.isEmpty)
        assertTrue(player.creature.right.isEmpty)
    }

    @Test
    fun addCardLeft_addsCardToCreatureLeft() {
        // Arrange
        val card = cards[0]

        // Act
        player.addCardLeft(card)

        // Assert
        assertEquals(listOf(CreatureCard(card)), player.creature.left.all)
        assertTrue(player.creature.left[0]!!.isFaceDown)
        assertTrue(player.creature.right.isEmpty)
    }

    @Test
    fun addCardRight_addsCardToCreatureRight() {
        // Arrange
        val card = cards[0]

        // Act
        player.addCardRight(card)

        // Assert
        assertEquals(listOf(CreatureCard(card)), player.creature.right.all)
        assertTrue(player.creature.right[0]!!.isFaceDown)
        assertTrue(player.creature.left.isEmpty)
    }

    @Test
    fun diceSupply_whenNew_isEmpty() {
        assertTrue(player.diceSupply.isEmpty())
    }

    @Test
    fun addDieToSupply_addsDieToSupplyOnly() {
        // Arrange
        val die = dice.d6

        // Act
        player.addDieToSupply(die)

        // Assert
        assertEquals(listOf(die), player.diceSupply.dice)
        assertTrue(player.diceHand.isEmpty())
        assertTrue(player.diceDiscard.isEmpty())
    }

    @Test
    fun addDiceToSupply_addsAllDiceToSupply() {
        // Arrange
        val incoming = listOf(dice.d8, dice.d4, dice.d6)

        // Act
        player.addDiceToSupply(incoming)

        // Assert
        assertEquals(incoming.sortedBy { it.sides }, player.diceSupply.dice.sortedBy { it.sides })
    }

    @Test
    fun drawDie_whenSupplyEmpty_returnsNull() {
        // Act
        val result = player.drawDie()

        // Assert
        assertNull(result)
        assertTrue(player.diceHand.isEmpty())
    }

    @Test
    fun drawDie_drawsLowestSidedDieFromSupplyToHand() {
        // Arrange
        val d8 = dice.d8
        val d4 = dice.d4
        val d12 = dice.d12
        player.addDiceToSupply(listOf(d8, d4, d12))

        // Act
        val result = player.drawDie()

        // Assert
        assertEquals(d4, result)
        assertEquals(listOf(d4), player.diceHand.dice)
        assertEquals(listOf(d8, d12).sortedBy { it.sides }, player.diceSupply.dice.sortedBy { it.sides })
    }

    @Test
    fun discardHandDice_movesAllHandDiceToDiscard() {
        // Arrange
        val drawn = mutableListOf<Die>()
        player.addDiceToSupply(listOf(dice.d4, dice.d6))
        player.drawDie()?.let { drawn.add(it) }
        player.drawDie()?.let { drawn.add(it) }

        // Act
        player.discardHandDice()

        // Assert
        assertTrue(player.diceHand.isEmpty())
        assertEquals(drawn.sortedBy { it.sides }, player.diceDiscard.dice.sortedBy { it.sides })
    }

    @Test
    fun clearDice_clearsSupplyHandAndDiscard() {
        // Arrange
        player.addDiceToSupply(listOf(dice.d4, dice.d6))
        player.drawDie()
        player.discardHandDice()
        player.addDieToSupply(dice.d8)

        // Act
        player.clearDice()

        // Assert
        assertTrue(player.diceSupply.isEmpty())
        assertTrue(player.diceHand.isEmpty())
        assertTrue(player.diceDiscard.isEmpty())
    }

}
