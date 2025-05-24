package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.game.turn.select.SelectDieToAdjust
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.player.effect.AppliedEffect
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HandleOpponentEffectsTest {
    companion object {
        private const val ADJUSTMENT = 2
        private const val DIE_VALUE = 4
        private const val PLAYER_ID = 1
        private const val OPPONENT_ID = 2

        private val ADJUST_DIE_ROLL_CANNOT_TARGET_EFFECT =
            AppliedEffect.AdjustDieRoll(adjustment = ADJUSTMENT, canTargetPlayer = false)
        private val ADJUST_DIE_ROLL_CAN_TARGET_EFFECT =
            AppliedEffect.AdjustDieRoll(adjustment = ADJUSTMENT, canTargetPlayer = true)
        private val DISCARD_CARDS_ONLY_EFFECT =
            AppliedEffect.Discard(cardsOnly = true, diceOnly = false, count = 1)
        private val DISCARD_DICE_ONLY_EFFECT =
            AppliedEffect.Discard(cardsOnly = false, diceOnly = true, count = 1)
        private val DISCARD_NO_PREFERENCE_EFFECT =
            AppliedEffect.Discard(cardsOnly = false, diceOnly = false, count = 1)
    }

    private lateinit var SUT: HandleOpponentEffects
    private lateinit var player: PlayerTD
    private lateinit var opponent: PlayerTD
    private lateinit var testDie: Die
    private lateinit var mockGameChronicle: GameChronicle
    private lateinit var mockSelectDieToAdjust: SelectDieToAdjust
    private lateinit var costScore: CostScore
    private val sampleDie = SampleDie()

    @BeforeEach
    fun setup() {
        // Create PlayerTD instances instead of mocks
        player = PlayerTD(PLAYER_ID)
        opponent = PlayerTD(OPPONENT_ID)
        
        mockGameChronicle = mockk(relaxed = true)
        mockSelectDieToAdjust = mockk(relaxed = true)
        costScore = mockk(relaxed = true)

        // Use SampleDie instead of mocked Die
        testDie = sampleDie.d6
        testDie.adjustTo(DIE_VALUE)
        
        // Add test die to opponent's hand
        opponent.diceInHand.add(testDie)
        
        // Add FakeCards to opponent's hand
        opponent.addCardToHand(FakeCards.fakeBloom)

        SUT = HandleOpponentEffects(
            mockSelectDieToAdjust,
            costScore,
            mockGameChronicle
        )

        // Setup SelectDieToAdjust mock to return the test die
        every { mockSelectDieToAdjust(any(), any()) } returns testDie
        
        // Start with a clean effectsList for each test
        player.effectsList.clear()
    }

    @Test
    fun invoke_whenEmptyEffectsList_doesNothing() {
        // Arrange - effectsList is already empty from setup

        // Capture initial state
        val initialHandSize = opponent.diceInHand.dice.size
        val initialCardsSize = opponent.cardsInHand.size

        // Act
        SUT(player, opponent)

        // Assert
        assertEquals(initialHandSize, opponent.diceInHand.dice.size)
        assertEquals(initialCardsSize, opponent.cardsInHand.size)
    }

    @Test
    fun invoke_whenAdjustDieRollCannotTargetEffect_doesNothing() {
        // Arrange - directly add to player's effectsList
        player.effectsList.add(ADJUST_DIE_ROLL_CANNOT_TARGET_EFFECT)

        // Capture initial state
        val initialHandSize = opponent.diceInHand.dice.size
        val initialCardsSize = opponent.cardsInHand.size

        // Act
        SUT(player, opponent)

        // Assert
        assertEquals(initialHandSize, opponent.diceInHand.dice.size)
        assertEquals(initialCardsSize, opponent.cardsInHand.size)
    }

    @Test
    fun invoke_whenAdjustDieRollCanTargetEffect_adjustsOpponentDieRoll() {
        // Arrange - directly add to player's effectsList
        player.effectsList.add(ADJUST_DIE_ROLL_CAN_TARGET_EFFECT)
        
        // Record initial value
        val initialValue = testDie.value

        // Act
        SUT(player, opponent)

        // Assert
        assertEquals(initialValue - ADJUSTMENT, testDie.value)
    }

    @Test
    fun invoke_whenDiscardCardsOnlyEffect_discardsLowestCostCard() {
        // Arrange - directly add to player's effectsList
        player.effectsList.add(DISCARD_CARDS_ONLY_EFFECT)

        // Act
        SUT(player, opponent)

        // Assert
        assertEquals(1, opponent.gotCardIds.size)
        assertEquals(FakeCards.fakeBloom.id, opponent.gotCardIds[0])
    }

    @Test
    fun invoke_whenDiscardDiceOnlyEffect_discardsLowestValueDie() {
        // Arrange - directly add to player's effectsList
        player.effectsList.add(DISCARD_DICE_ONLY_EFFECT)

        // Act
        SUT(player, opponent)

        // Assert
        assertEquals(0, opponent.diceInHand.dice.size)
        assertEquals(1, opponent.diceInCompost.dice.size)
        assertEquals(testDie.sides, opponent.diceInCompost.dice[0].sides)
    }

    @Test
    fun invoke_whenDiscardNoPreferenceEffect_discardsLowestValueDie() {
        // Arrange - directly add to player's effectsList
        player.effectsList.add(DISCARD_NO_PREFERENCE_EFFECT)
        
        // Setup a low value die for this test
        val lowValueDie = sampleDie.d6
        lowValueDie.adjustTo(2)
        
        // Add another die to hand
        opponent.addDieToHand(lowValueDie)

        // Act
        SUT(player, opponent)

        // Assert
        assertEquals(1, opponent.diceInHand.dice.size)
        assertEquals(1, opponent.diceInCompost.dice.size)
    }

    @Test
    fun invoke_whenMultipleEffects_processesAllEffects() {
        // Arrange - directly add to player's effectsList
        player.effectsList.add(DISCARD_CARDS_ONLY_EFFECT)
        player.effectsList.add(ADJUST_DIE_ROLL_CAN_TARGET_EFFECT)

        // Act
        SUT(player, opponent)

        // Assert
        // Card should be discarded
        assertEquals(1, opponent.gotCardIds.size)
        assertEquals(FakeCards.fakeBloom.id, opponent.gotCardIds[0])
        
        // Die should be adjusted
        assertEquals(DIE_VALUE - ADJUSTMENT, testDie.value)
    }

    @Test
    fun invoke_whenAdjustDieRollEffect_noSuitableDie_doesNothing() {
        // Arrange - directly add to player's effectsList
        player.effectsList.add(ADJUST_DIE_ROLL_CAN_TARGET_EFFECT)
        
        // Setup a die with value 1 which is too low to adjust
        val lowValueDie = sampleDie.d6
        lowValueDie.adjustTo(1)
        
        // Clear and set up dice in hand
        opponent.diceInHand.clear()
        opponent.diceInHand.add(lowValueDie)

        // Act
        SUT(player, opponent)

        // Assert
        assertEquals(1, opponent.diceInHand.dice.size)
        assertEquals(1, opponent.diceInHand.dice[0].value)
    }

    @Test
    fun invoke_whenDiscardEffect_noCardsOrDice_doesNothing() {
        // Arrange - directly add to player's effectsList
        player.effectsList.add(DISCARD_NO_PREFERENCE_EFFECT)
        opponent.removeCardFromHand(FakeCards.fakeBloom)
        opponent.removeDieFromHand(testDie)

        // Act
        SUT(player, opponent)

        // Assert
        assertEquals(0, opponent.diceInHand.dice.size)
        assertEquals(0, opponent.cardsInHand.size)
        assertEquals(0, opponent.diceInCompost.dice.size)
        assertEquals(0, opponent.gotCardIds.size)
    }
} 
