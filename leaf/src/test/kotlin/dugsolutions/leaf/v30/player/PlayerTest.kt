package dugsolutions.leaf.v30.player

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.common.Butterfly
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.components.CreatureCard
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.SampleDie
import dugsolutions.leaf.v30.wisp.WispCardRegistry
import dugsolutions.leaf.v30.wisp.domain.WispCard
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerTest {

    private lateinit var cards: List<GameCard>
    private lateinit var wisps: List<WispCard>
    private lateinit var dice: SampleDie
    private lateinit var player: Player

    @BeforeEach
    fun setup() {
        val registry = GameCardRegistry()
        registry.loadFromCsv(Commons.CARD_LIST)
        cards = registry.getAllCards()
        val wispRegistry = WispCardRegistry()
        wispRegistry.loadFromCsv(Commons.WISP_LIST)
        wisps = wispRegistry.getAllCards()
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

    @Test
    fun critters_whenNew_isEmpty() {
        assertEquals(emptyList(), player.critters)
    }

    @Test
    fun addCritter_addsCritterToPlayer() {
        // Act
        player.addCritter(Critter.BEE)

        // Assert
        assertEquals(listOf(Critter.BEE), player.critters)
    }

    @Test
    fun addCritter_withMultipleCritters_preservesOrder() {
        // Act
        player.addCritter(Critter.BEE)
        player.addCritter(Critter.WORM)

        // Assert
        assertEquals(listOf(Critter.BEE, Critter.WORM), player.critters)
    }

    @Test
    fun removeCritter_whenCritterExists_removesFirstMatch() {
        // Arrange
        player.addCritter(Critter.BEE)
        player.addCritter(Critter.WORM)
        player.addCritter(Critter.BEE)

        // Act
        val result = player.removeCritter(Critter.BEE)

        // Assert
        assertTrue(result)
        assertEquals(listOf(Critter.WORM, Critter.BEE), player.critters)
    }

    @Test
    fun removeCritter_whenCritterDoesNotExist_returnsFalse() {
        // Arrange
        player.addCritter(Critter.BEE)

        // Act
        val result = player.removeCritter(Critter.WORM)

        // Assert
        assertEquals(false, result)
        assertEquals(listOf(Critter.BEE), player.critters)
    }

    @Test
    fun critters_whenSnapshotIsChanged_doesNotChangePlayerCritters() {
        // Arrange
        player.addCritter(Critter.BEE)
        val snapshot = player.critters.toMutableList()

        // Act
        snapshot.clear()

        // Assert
        assertEquals(listOf(Critter.BEE), player.critters)
    }

    @Test
    fun butterflies_whenNew_isEmpty() {
        assertEquals(emptyList(), player.butterflies)
    }

    @Test
    fun addButterfly_addsButterflyToPlayer() {
        // Act
        player.addButterfly(Butterfly.GREEN)

        // Assert
        assertEquals(listOf(Butterfly.GREEN), player.butterflies)
    }

    @Test
    fun addButterfly_withMultipleButterflies_preservesOrder() {
        // Act
        player.addButterfly(Butterfly.GREEN)
        player.addButterfly(Butterfly.YELLOW)
        player.addButterfly(Butterfly.RED)
        player.addButterfly(Butterfly.PURPLE)

        // Assert
        assertEquals(
            listOf(Butterfly.GREEN, Butterfly.YELLOW, Butterfly.RED, Butterfly.PURPLE),
            player.butterflies
        )
    }

    @Test
    fun removeButterfly_whenButterflyExists_removesFirstMatch() {
        // Arrange
        player.addButterfly(Butterfly.GREEN)
        player.addButterfly(Butterfly.PURPLE)
        player.addButterfly(Butterfly.GREEN)

        // Act
        val result = player.removeButterfly(Butterfly.GREEN)

        // Assert
        assertTrue(result)
        assertEquals(listOf(Butterfly.PURPLE, Butterfly.GREEN), player.butterflies)
    }

    @Test
    fun removeButterfly_whenButterflyDoesNotExist_returnsFalse() {
        // Arrange
        player.addButterfly(Butterfly.YELLOW)

        // Act
        val result = player.removeButterfly(Butterfly.RED)

        // Assert
        assertEquals(false, result)
        assertEquals(listOf(Butterfly.YELLOW), player.butterflies)
    }

    @Test
    fun butterflies_whenSnapshotIsChanged_doesNotChangePlayerButterflies() {
        // Arrange
        player.addButterfly(Butterfly.GREEN)
        val snapshot = player.butterflies.toMutableList()

        // Act
        snapshot.clear()

        // Assert
        assertEquals(listOf(Butterfly.GREEN), player.butterflies)
    }

    @Test
    fun wispCards_whenNew_isEmpty() {
        assertEquals(emptyList(), player.wispCards.cards)
    }

    @Test
    fun addWispCard_addsWispCardToPlayer() {
        // Arrange
        val wisp = wisps[0]

        // Act
        player.addWispCard(wisp)

        // Assert
        assertEquals(listOf(wisp), player.wispCards.cards)
    }

    @Test
    fun addWispCard_withMultipleWisps_preservesOrder() {
        // Arrange
        val first = wisps[0]
        val second = wisps[1]

        // Act
        player.addWispCard(first)
        player.addWispCard(second)

        // Assert
        assertEquals(listOf(first, second), player.wispCards.cards)
    }

    @Test
    fun removeWispCard_whenCardExists_removesFirstMatch() {
        // Arrange
        val first = wisps[0]
        val second = wisps[1]
        player.addWispCard(first)
        player.addWispCard(second)
        player.addWispCard(first)

        // Act
        val result = player.removeWispCard(first)

        // Assert
        assertTrue(result)
        assertEquals(listOf(second, first), player.wispCards.cards)
    }

    @Test
    fun removeWispCard_whenCardDoesNotExist_returnsFalse() {
        // Arrange
        player.addWispCard(wisps[0])

        // Act
        val result = player.removeWispCard(wisps[1])

        // Assert
        assertEquals(false, result)
        assertEquals(listOf(wisps[0]), player.wispCards.cards)
    }

    @Test
    fun wispCards_whenSnapshotIsChanged_doesNotChangePlayerWisps() {
        // Arrange
        val wisp = wisps[0]
        player.addWispCard(wisp)
        val snapshot = player.wispCards.cards.toMutableList()

        // Act
        snapshot.clear()

        // Assert
        assertEquals(listOf(wisp), player.wispCards.cards)
    }

    @Test
    fun vp_whenNew_isZero() {
        assertEquals(0, player.vp)
    }

    @Test
    fun addVp_increasesPlayerVp() {
        player.addVp(1)
        player.addVp(3)

        assertEquals(4, player.vp)
    }

    @Test
    fun addVp_withNonPositiveAmount_throwsException() {
        assertThrows<IllegalArgumentException> {
            player.addVp(0)
        }
        assertThrows<IllegalArgumentException> {
            player.addVp(-1)
        }
    }

    @Test
    fun resetVp_setsPlayerVpBackToZero() {
        player.addVp(5)

        player.resetVp()

        assertEquals(0, player.vp)
    }

}
