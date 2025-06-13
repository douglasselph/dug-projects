package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.SampleDie
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EffectDiscardTest {
    private val player = PlayerTD.create2(1)
    private val costScore: CostScore = mockk()
    private val chronicle: GameChronicle = mockk(relaxed = true)

    private val SUT: EffectDiscard = EffectDiscard(costScore, chronicle)

    private val sampleDie = SampleDie()
    private val dieLow: Die = sampleDie.d6.adjustTo(1)
    private val dieHigh: Die = sampleDie.d6.adjustTo(5)
    private val cardLow = FakeCards.fakeSeedling
    private val cardHigh = FakeCards.fakeVine

    @BeforeEach
    fun setup() {
    }

    @Test
    fun invoke_cards_discardsLowestCostCard_andCallsChronicle() {
        // Arrange
        player.addCardToHand(cardLow)
        player.addCardToHand(cardHigh)
        every { costScore(cardLow.cost) } returns 1
        every { costScore(cardHigh.cost) } returns 10

        // Act
        SUT(EffectDiscard.DiscardWhich.CARDS, player)

        // Assert
        verify { player.discard(cardLow.id) }
        verify { chronicle(Moment.DISCARD_CARD(player, cardLow)) }
    }

    @Test
    fun invoke_dice_discardsLowestValueDie_andCallsChronicle() {
        // Arrange
        player.addDieToHand(dieLow)
        player.addDieToHand(dieHigh)

        // Act
        SUT(EffectDiscard.DiscardWhich.DICE, player)

        // Assert
        verify { player.discard(dieLow) }
        verify { chronicle(Moment.DISCARD_DIE(player, dieLow)) }
    }

    @Test
    fun invoke_both_withLowDice_discardsLowestValueLowDie_andCallsChronicle() {
        // Arrange
        player.addDieToHand(dieLow)
        player.addDieToHand(dieHigh)
        player.addCardToHand(cardLow)
        every { costScore(cardLow.cost) } returns 1

        // Act
        SUT(EffectDiscard.DiscardWhich.BOTH, player)

        // Assert
        verify { player.discard(dieLow) }
        verify { chronicle(Moment.DISCARD_DIE(player, dieLow)) }
    }

    @Test
    fun invoke_both_withNoLowDice_discardsLowestCostCard_andCallsChronicle() {
        // Arrange
        player.addDieToHand(dieHigh)
        player.addCardToHand(cardLow)
        player.addCardToHand(cardHigh)
        every { costScore(cardLow.cost) } returns 1
        every { costScore(cardHigh.cost) } returns 10

        // Act
        SUT(EffectDiscard.DiscardWhich.BOTH, player)

        // Assert
        verify { player.discard(cardLow.id) }
        verify { chronicle(Moment.DISCARD_CARD(player, cardLow)) }
    }

    @Test
    fun invoke_cards_withNoCards_doesNothing() {
        // Arrange
        // No cards in hand
        // Act
        SUT(EffectDiscard.DiscardWhich.CARDS, player)
        // Assert
        verify(exactly = 0) { chronicle(any()) }
    }

    @Test
    fun invoke_dice_withNoDice_doesNothing() {
        // Arrange
        // No dice in hand
        // Act
        SUT(EffectDiscard.DiscardWhich.DICE, player)
        // Assert
        verify(exactly = 0) { chronicle(any()) }
    }

    @Test
    fun invoke_both_withNoLowDiceOrCards_doesNothing() {
        // Arrange
        // No dice or cards in hand
        // Act
        SUT(EffectDiscard.DiscardWhich.BOTH, player)
        // Assert
        verify(exactly = 0) { chronicle(any()) }
    }
} 
