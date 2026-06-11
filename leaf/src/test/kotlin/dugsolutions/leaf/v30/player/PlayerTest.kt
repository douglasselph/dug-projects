package dugsolutions.leaf.v30.player

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.common.Butterfly
import dugsolutions.leaf.v30.common.Commons
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.player.domain.CreatureCard
import dugsolutions.leaf.v30.player.domain.OutOfDiceException
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieSides
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
        assertTrue(player.isCreatureLeftEmpty)
        assertTrue(player.isCreatureRightEmpty)
    }

    @Test
    fun addCardLeft_addsCardToCreatureLeft() {
        // Arrange
        val card = cards[0]

        // Act
        player.addCardLeft(card)

        // Assert
        assertEquals(listOf(CreatureCard(card)), player.creatureLeftCards)
        assertTrue(player.getCreatureLeftCard(0)!!.isFaceDown)
        assertTrue(player.isCreatureRightEmpty)
    }

    @Test
    fun addCardRight_addsCardToCreatureRight() {
        // Arrange
        val card = cards[0]

        // Act
        player.addCardRight(card)

        // Assert
        assertEquals(listOf(CreatureCard(card)), player.creatureRightCards)
        assertTrue(player.getCreatureRightCard(0)!!.isFaceDown)
        assertTrue(player.isCreatureLeftEmpty)
    }

    @Test
    fun flipItOrSnipIt_whenChosenCardIsFaceUp_flipsCardFaceDownAndRecordsChronicle() {
        val chronicle = GameChronicle()
        val target = Player(chronicle = chronicle, id = 10)
        val card = cards[0]
        target.addCardToCreature(CreatureCard(card, CreatureCard.Facing.FACE_UP))

        target.flipItOrSnipIt()

        assertEquals(true, target.creatureCards.single().isFaceDown)
        val entry = chronicle.getEntries().single() as GameEntry.WoundCard
        assertEquals(10, entry.playerId)
        assertEquals(card.name, entry.cardName)
        assertEquals(true, entry.wasFlipped)
        assertEquals(false, entry.wasLost)
    }

    @Test
    fun flipItOrSnipIt_whenChosenCardIsFaceDown_removesCardAndRecordsChronicle() {
        val chronicle = GameChronicle()
        val target = Player(chronicle = chronicle, id = 11)
        val card = cards[0]
        target.addCardToCreature(CreatureCard(card, CreatureCard.Facing.FACE_DOWN))

        target.flipItOrSnipIt()

        assertEquals(emptyList(), target.creatureCards)
        val entry = chronicle.getEntries().single() as GameEntry.WoundCard
        assertEquals(11, entry.playerId)
        assertEquals(card.name, entry.cardName)
        assertEquals(false, entry.wasFlipped)
        assertEquals(true, entry.wasLost)
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
    fun drawDiceWithRefresh_whenSupplyHasDie_drawsFromSupply() {
        // Arrange
        val die = dice.d6
        player.addDieToSupply(die)

        // Act
        val result = player.drawDiceWithRefresh()

        // Assert
        assertEquals(die, result)
        assertEquals(listOf(die), player.diceHand.dice)
        assertTrue(player.diceSupply.isEmpty())
    }

    @Test
    fun drawDiceWithRefresh_whenSupplyEmpty_refreshesFromDiscardThenDraws() {
        // Arrange
        val d4 = dice.d4
        val d6 = dice.d6
        player.addDiceToSupply(listOf(d4, d6))
        player.drawDie()
        player.drawDie()
        player.discardHandDice()

        // Act
        val result = player.drawDiceWithRefresh()

        // Assert
        assertEquals(d4, result)
        assertEquals(listOf(d4), player.diceHand.dice)
        assertEquals(listOf(d6), player.diceSupply.dice)
        assertTrue(player.diceDiscard.isEmpty())
    }

    @Test
    fun drawDiceWithRefresh_whenSupplyAndDiscardEmpty_throwsException() {
        assertThrows<OutOfDiceException> {
            player.drawDiceWithRefresh()
        }
    }

    @Test
    fun rollDice_rollsDiceInHandOnly() {
        // Arrange
        val handDie = TrackingDie(6)
        val supplyDie = TrackingDie(8)
        player.addDieToSupply(handDie)
        player.drawDie()
        player.addDieToSupply(supplyDie)

        // Act
        player.rollDice()

        // Assert
        assertEquals(1, handDie.rollCount)
        assertEquals(0, supplyDie.rollCount)
    }

    @Test
    fun addDieToHand_addsDieDirectlyToHand() {
        val die = dice.d8

        player.addDieToHand(die)

        assertEquals(listOf(die), player.diceHand.dice)
        assertTrue(player.diceSupply.isEmpty())
        assertTrue(player.diceDiscard.isEmpty())
    }

    @Test
    fun rerollDie_whenMatchingDieIsInHand_rollsStoredDieAndReturnsDie() {
        val die = TrackingDie(6)
        player.addDieToHand(die)

        val result = player.rerollDie(FixedDie(6, 1))

        assertEquals(die, result)
        assertEquals(1, die.rollCount)
    }

    @Test
    fun rerollDie_whenMatchingDieIsNotInHand_returnsNull() {
        player.addDieToHand(TrackingDie(6))

        val result = player.rerollDie(FixedDie(8, 1))

        assertNull(result)
    }

    @Test
    fun raiseDie_whenMatchingDieIsInHand_raisesStoredDieAndReturnsDie() {
        val die = FixedDie(6, 5)
        player.addDieToHand(die)

        val result = player.raiseDie(FixedDie(6, 5), 3)

        assertEquals(die, result)
        assertEquals(6, die.value)
    }

    @Test
    fun raiseDie_whenMatchingDieIsNotInHand_returnsNull() {
        player.addDieToHand(FixedDie(6, 5))

        val result = player.raiseDie(FixedDie(8, 5), 1)

        assertNull(result)
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
    fun tokens_whenNew_areEmpty() {
        assertEquals(0, player.waterTokenCount)
        assertEquals(emptyList(), player.mulchTokens)
    }

    @Test
    fun add_withWaterToken_incrementsWaterTokenCount() {
        player.add(Token.WATER)

        assertEquals(1, player.waterTokenCount)
    }

    @Test
    fun add_withMulchToken_addsExactMulchToken() {
        val token = Token.MULCH(DieSides.D8)

        player.add(token)

        assertEquals(listOf(token), player.mulchTokens)
    }

    @Test
    fun remove_withExactToken_removesTokenAndReturnsTrue() {
        val token = Token.MULCH(DieSides.D8)
        player.add(token)

        val result = player.remove(token)

        assertTrue(result)
        assertEquals(emptyList(), player.mulchTokens)
    }

    @Test
    fun remove_withNonMatchingMulchToken_returnsFalse() {
        player.add(Token.MULCH())

        val result = player.remove(Token.MULCH(DieSides.D8))

        assertEquals(false, result)
        assertEquals(listOf(Token.MULCH()), player.mulchTokens)
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

    private class TrackingDie(sides: Int) : Die(sides) {
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

        override fun roll(): Die = this
    }

}
