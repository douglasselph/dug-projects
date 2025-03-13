package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.GameCard
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DecisionShouldProcessTrashEffectCoreStrategyTest {

    companion object {
        private val fakeCard = FakeCards.fakeSeedling
        private val fakeVineUnsupported = FakeCards.fakeVine
        private val fakeNoEffect = FakeCards.fakeRoot
    }

    private lateinit var SUT: DecisionShouldProcessTrashEffectCoreStrategy

    @BeforeEach
    fun setup() {
        SUT = DecisionShouldProcessTrashEffectCoreStrategy()
        assertTrue(fakeNoEffect.trashEffect == null)
        assertTrue(fakeCard.trashEffect != null)
        assertTrue(fakeVineUnsupported.trashEffect != null)
    }

    @Test
    fun invoke_whenFirstTimeSeeingCard_returnsFalse() {
        // Act
        val result = SUT(fakeCard)

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenSecondTimeSeeingCard_returnsFalse() {
        // Arrange
        SUT(fakeCard)

        // Act
        val result = SUT(fakeCard)

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenThirdTimeSeeingCard_returnsTrue() {
        // Arrange
        SUT(fakeCard)
        SUT(fakeCard)

        // Act
        val result = SUT(fakeCard)

        // Assert
        assertTrue(result)
    }

    @Test
    fun invoke_whenFourthTimeSeeingCard_returnsTrue() {
        // Arrange
        SUT(fakeCard)
        SUT(fakeCard)
        SUT(fakeCard)

        // Act
        val result = SUT(fakeCard)

        // Assert
        assertTrue(result)
    }

    @Test
    fun invoke_whenDifferentCard_tracksSeparately() {
        // Arrange
        val differentCard = FakeCards.fakeBloom
        assertTrue(differentCard.trashEffect != null)
        SUT.trashTriggerGeneral = 3
        SUT(fakeCard)
        SUT(fakeCard)

        // Act
        val differentCardFirstTime = SUT(differentCard)
        val fakeCardThirdTime = SUT(fakeCard)
        val differentCardSecondTime = SUT(differentCard)
        val differentCardThirdTime = SUT(differentCard)

        // Assert
        assertFalse(differentCardFirstTime, "First time seeing different card should be false")
        assertTrue(fakeCardThirdTime, "Third time for first card should be true")
        assertFalse(differentCardSecondTime, "Second time for different card should be false")
        assertTrue(differentCardThirdTime, "Third time for different card should be true")
    }

    @Test
    fun invoke_whenCardHasSpecificTrigger_usesThatTrigger() {
        // Arrange
        SUT.trashTriggerCard[fakeCard.id] = 2 // Set specific trigger to 2

        // Act
        val firstTime = SUT(fakeCard)
        val secondTime = SUT(fakeCard)
        val thirdTime = SUT(fakeCard)

        // Assert
        assertFalse(firstTime, "First time should be false")
        assertTrue(secondTime, "Second time should be true")
        assertTrue(thirdTime, "Third time should be true")
    }

    @Test
    fun invoke_whenGeneralTriggerIsOne_trashesOnFirstSight() {
        // Arrange
        SUT.trashTriggerGeneral = 1

        // Act
        val result = SUT(fakeCard)

        // Assert
        assertTrue(result, "Should trash on first sight when general trigger is 1")
    }

    @Test
    fun invoke_whenGeneralTriggerIsTwo_trashesOnSecondSight() {
        // Arrange
        SUT.trashTriggerGeneral = 2

        // Act
        val firstTime = SUT(fakeCard)
        val secondTime = SUT(fakeCard)

        // Assert
        assertFalse(firstTime, "First time should be false")
        assertTrue(secondTime, "Second time should be true")
    }

    @Test
    fun reset_clearsTracking() {
        // Arrange
        SUT(fakeCard)
        SUT(fakeCard)

        // Act
        SUT.reset()
        val result = SUT(fakeCard)

        // Assert
        assertFalse(result, "After reset, should be like seeing card for first time")
    }

    @Test
    fun invoke_afterReset_startsTrackingAgain() {
        // Arrange
        SUT(fakeCard)
        SUT(fakeCard)
        SUT.reset()
        SUT(fakeCard)

        // Act
        val secondTime = SUT(fakeCard)
        val thirdTime = SUT(fakeCard)

        // Assert
        assertFalse(secondTime, "Second time after reset should still be false")
        assertTrue(thirdTime, "Third time after reset should be true")
    }

    @Test
    fun invoke_whenCardHasNoEffects_alwaysReturnsTrue() {
        // Arrange
        val cardWithoutEffects = mockk<GameCard>(relaxed = true)
        every { cardWithoutEffects.id } returns fakeCard.id
        every { cardWithoutEffects.primaryEffect } returns null
        every { cardWithoutEffects.matchEffect } returns null
        every { cardWithoutEffects.trashEffect } returns CardEffect.UPGRADE_D6

        // Act
        val firstTime = SUT(cardWithoutEffects)
        val secondTime = SUT(cardWithoutEffects)
        val thirdTime = SUT(cardWithoutEffects)

        // Assert
        assertTrue(firstTime, "First time seeing card without effects should be true")
        assertTrue(secondTime, "Second time seeing card without effects should be true")
        assertTrue(thirdTime, "Third time seeing card without effects should be true")
    }

    @Test
    fun invoke_whenCardHasNoTrashEffect_returnsFalse() {
        // Act
        val result = SUT(fakeNoEffect)
        
        // Assert
        assertFalse(result, "Card with no trash effect should return false")
    }

    @Test
    fun invoke_whenCardHasUnsupportedTrashEffect_returnsFalse() {
        // Act
        val firstTime = SUT(fakeVineUnsupported)
        val secondTime = SUT(fakeVineUnsupported)
        val thirdTime = SUT(fakeVineUnsupported)
        
        // Assert
        assertFalse(firstTime, "First time seeing card with unsupported trash effect should be false")
        assertFalse(secondTime, "Second time seeing card with unsupported trash effect should be false")
        assertFalse(thirdTime, "Third time seeing card with unsupported trash effect should be false")
    }

    @Test
    fun invoke_whenCardHasSupportedTrashEffect_followsNormalTracking() {
        // Act
        val firstTime = SUT(fakeCard)
        val secondTime = SUT(fakeCard)
        val thirdTime = SUT(fakeCard)
        
        // Assert
        assertFalse(firstTime, "First time seeing card with supported trash effect should be false")
        assertFalse(secondTime, "Second time seeing card with supported trash effect should be false")
        assertTrue(thirdTime, "Third time seeing card with supported trash effect should be true")
    }

} 
