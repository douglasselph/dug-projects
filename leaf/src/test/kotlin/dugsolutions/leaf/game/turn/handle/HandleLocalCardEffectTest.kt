package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.CardOrDie
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.game.turn.select.SelectCardToRetain
import dugsolutions.leaf.game.turn.select.SelectDieToReroll
import dugsolutions.leaf.game.turn.select.SelectDieToRetain
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.AppliedEffect
import dugsolutions.leaf.player.effect.EffectsList
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HandleLocalCardEffectTest {

    companion object {
        private const val CARD_ID_1 = 1

        private val REROLL_DIE_EFFECT = AppliedEffect.RerollDie(count = 1)
        private val RETAIN_CARD_EFFECT = AppliedEffect.RetainCard()
        private val RETAIN_DIE_EFFECT = AppliedEffect.RetainDie()
        private val REUSE_EFFECT = AppliedEffect.Reuse(flourishType = FlourishType.BLOOM, cardOrDie = CardOrDie.Card)
        private val UPGRADE_DIE_EFFECT = AppliedEffect.UpgradeDie(discardAfterUse = false)
        private val SMALL_UPGRADE_DIE_EFFECT = AppliedEffect.UpgradeDie(only = listOf(DieSides.D4, DieSides.D6), discardAfterUse = false)
        private val THORN_EFFECT = AppliedEffect.ThornEffect(damage = 2)
        private val DEFLECT_DAMAGE_EFFECT = AppliedEffect.DeflectDamage(amount = 2)
    }

    // Subject under test
    private lateinit var SUT: HandleLocalCardEffect
    
    // Dependencies
    private lateinit var mockSelectDieToReroll: SelectDieToReroll
    private lateinit var mockSelectCardToRetain: SelectCardToRetain
    private lateinit var mockSelectDieToRetain: SelectDieToRetain
    private lateinit var mockHandleDieUpgrade: HandleDieUpgrade
    private lateinit var mockHandleLimitedDieUpgrade: HandleLimitedDieUpgrade
    private lateinit var mockChronicle: GameChronicle
    
    // Test data
    private lateinit var mockPlayer: Player
    private lateinit var mockDie: Die
    private lateinit var mockDice: Dice
    private lateinit var mockCard: GameCard

    @BeforeEach
    fun setup() {
        // Create mocks
        mockSelectDieToReroll = mockk(relaxed = true)
        mockSelectCardToRetain = mockk(relaxed = true)
        mockSelectDieToRetain = mockk(relaxed = true)
        mockHandleDieUpgrade = mockk(relaxed = true)
        mockHandleLimitedDieUpgrade = mockk(relaxed = true)
        mockChronicle = mockk(relaxed = true)
        
        mockPlayer = mockk(relaxed = true)
        mockDie = mockk(relaxed = true)
        mockDice = mockk(relaxed = true)
        mockCard = mockk(relaxed = true)

        // Create subject under test with correct constructor arguments
        SUT = HandleLocalCardEffect(
            mockSelectDieToReroll,
            mockSelectCardToRetain,
            mockSelectDieToRetain,
            mockHandleDieUpgrade,
            mockHandleLimitedDieUpgrade,
            mockChronicle
        )
        
        // Set up default mock behaviors
        every { mockPlayer.diceInHand } returns mockDice
        every { mockDie.roll() } returns mockDie
        every { mockPlayer.cardsReused } returns mutableListOf()
        every { mockCard.id } returns CARD_ID_1
        every { mockPlayer.retainCard(any()) } returns true
        every { mockPlayer.retainDie(any()) } returns true
        every { mockChronicle(any()) } just Runs
    }

    @Test
    fun invoke_whenNoEffects_doesNothing() {
        // Arrange
        val effectsList = mockk<EffectsList>(relaxed = true) {
            every { copy() } returns emptyList()
        }
        every { mockPlayer.effectsList } returns effectsList

        // Act
        SUT(mockPlayer)

        // Assert
        verify(exactly = 0) { mockSelectDieToReroll(any()) }
        verify(exactly = 0) { mockSelectCardToRetain(any(), any()) }
        verify(exactly = 0) { mockSelectDieToRetain(any()) }
        verify(exactly = 0) { mockHandleDieUpgrade(any(), any()) }
        verify(exactly = 0) { mockHandleLimitedDieUpgrade(any(), any(), any()) }
    }

    @Test
    fun invoke_whenRerollDieEffect_rerollsDie() {
        // Arrange
        val effectsList = mockk<EffectsList>(relaxed = true) {
            every { copy() } returns listOf(REROLL_DIE_EFFECT)
            every { remove(any()) } just Runs
        }
        every { mockPlayer.effectsList } returns effectsList
        every { mockSelectDieToReroll(any()) } returns mockDie

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockDie.roll() }
        verify { effectsList.remove(REROLL_DIE_EFFECT) }
        verify { mockChronicle(any<GameChronicle.Moment.REROLL>()) }
    }

    @Test
    fun invoke_whenRetainCardEffect_retainsCard() {
        // Arrange
        val effectsList = mockk<EffectsList>(relaxed = true) {
            every { copy() } returns listOf(RETAIN_CARD_EFFECT)
            every { remove(any()) } just Runs
        }
        every { mockPlayer.effectsList } returns effectsList
        every { mockPlayer.cardsInHand } returns listOf(mockCard)
        every { mockSelectCardToRetain(any(), null) } returns mockCard

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.retainCard(CARD_ID_1) }
        verify { effectsList.remove(RETAIN_CARD_EFFECT) }
        verify { mockChronicle(any<GameChronicle.Moment.RETAIN_CARD>()) }
    }

    @Test
    fun invoke_whenRetainDieEffect_retainsDie() {
        // Arrange
        val effectsList = mockk<EffectsList>(relaxed = true) {
            every { copy() } returns listOf(RETAIN_DIE_EFFECT)
            every { remove(any()) } just Runs
        }
        every { mockPlayer.effectsList } returns effectsList
        every { mockSelectDieToRetain(any()) } returns mockDie

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.retainDie(mockDie) }
        verify { effectsList.remove(RETAIN_DIE_EFFECT) }
        verify { mockChronicle(any<GameChronicle.Moment.RETAIN_DIE>()) }
    }

    @Test
    fun invoke_whenReuseEffect_addsToReusedCards() {
        // Arrange
        val effectsList = mockk<EffectsList>(relaxed = true) {
            every { copy() } returns listOf(REUSE_EFFECT)
            every { remove(any()) } just Runs
        }
        val cardsReused = mockk<MutableList<GameCard>>(relaxed = true)
        every { mockPlayer.effectsList } returns effectsList
        every { mockPlayer.cardsInHand } returns listOf(mockCard)
        every { mockPlayer.cardsReused } returns cardsReused
        every { mockSelectCardToRetain(any(), FlourishType.BLOOM) } returns mockCard

        // Act
        SUT(mockPlayer)

        // Assert
        verify { cardsReused.add(mockCard) }
        verify { effectsList.remove(REUSE_EFFECT) }
        verify { mockChronicle(any<GameChronicle.Moment.REUSE_CARD>()) }
    }

    @Test
    fun invoke_whenUpgradeDieEffect_upgradesDie() {
        // Arrange
        val effectsList = mockk<EffectsList>(relaxed = true) {
            every { copy() } returns listOf(UPGRADE_DIE_EFFECT)
            every { remove(any()) } just Runs
        }
        every { mockPlayer.effectsList } returns effectsList
        every { mockHandleDieUpgrade(mockPlayer, any()) } returns mockDie

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockHandleDieUpgrade(mockPlayer, false) }
        verify { effectsList.remove(UPGRADE_DIE_EFFECT) }
        verify { mockChronicle(any<GameChronicle.Moment.UPGRADE_DIE>()) }
    }

    @Test
    fun invoke_whenSmallUpgradeDieEffect_upgradesSmallDie() {
        // Arrange
        val effectsList = mockk<EffectsList>(relaxed = true) {
            every { copy() } returns listOf(SMALL_UPGRADE_DIE_EFFECT)
            every { remove(any()) } just Runs
        }
        every { mockPlayer.effectsList } returns effectsList
        every { mockHandleLimitedDieUpgrade(mockPlayer, any(), any()) } returns mockDie

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockHandleLimitedDieUpgrade(mockPlayer, any(), false) }
        verify { effectsList.remove(SMALL_UPGRADE_DIE_EFFECT) }
        verify { mockChronicle(any<GameChronicle.Moment.UPGRADE_DIE>()) }
    }

    @Test
    fun invoke_whenThornEffect_addsDamage() {
        // Arrange
        val effectsList = mockk<EffectsList>(relaxed = true) {
            every { copy() } returns listOf(THORN_EFFECT)
            every { remove(any()) } just Runs
        }
        every { mockPlayer.effectsList } returns effectsList
        every { mockPlayer.thornDamage } returns 0
        every { mockPlayer.thornDamage = any() } just Runs

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.thornDamage += 2 }
        verify { effectsList.remove(THORN_EFFECT) }
        verify { mockChronicle(any<GameChronicle.Moment.ADD_TO_THORN>()) }
    }

    @Test
    fun invoke_whenDeflectDamageEffect_addsDeflection() {
        // Arrange
        val effectsList = mockk<EffectsList>(relaxed = true) {
            every { copy() } returns listOf(DEFLECT_DAMAGE_EFFECT)
            every { remove(any()) } just Runs
        }
        every { mockPlayer.effectsList } returns effectsList
        every { mockPlayer.deflectDamage } returns 0
        every { mockPlayer.deflectDamage = any() } just Runs

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.deflectDamage += 2 }
        verify { effectsList.remove(DEFLECT_DAMAGE_EFFECT) }
        verify { mockChronicle(any<GameChronicle.Moment.DEFLECT_DAMAGE>()) }
    }

    @Test
    fun invoke_multipleEffects_processesAllEffects() {
        // Arrange
        val allEffects = listOf(
            REROLL_DIE_EFFECT,
            RETAIN_DIE_EFFECT,
            THORN_EFFECT
        )
        
        val effectsList = mockk<EffectsList>(relaxed = true) {
            every { copy() } returns allEffects
            every { remove(any()) } just Runs
        }
        
        every { mockPlayer.effectsList } returns effectsList
        every { mockSelectDieToReroll(any()) } returns mockDie
        every { mockSelectDieToRetain(any()) } returns mockDie
        
        // Act
        SUT(mockPlayer)
        
        // Assert
        verify { mockDie.roll() }
        verify { mockPlayer.retainDie(mockDie) }
        verify { mockPlayer.thornDamage += 2 }
        
        // Verify all effects were removed
        verify { effectsList.remove(REROLL_DIE_EFFECT) }
        verify { effectsList.remove(RETAIN_DIE_EFFECT) }
        verify { effectsList.remove(THORN_EFFECT) }
        
        // Verify chronicle was called for each effect
        verify { mockChronicle(any<GameChronicle.Moment.REROLL>()) }
        verify { mockChronicle(any<GameChronicle.Moment.RETAIN_DIE>()) }
        verify { mockChronicle(any<GameChronicle.Moment.ADD_TO_THORN>()) }
    }
} 
