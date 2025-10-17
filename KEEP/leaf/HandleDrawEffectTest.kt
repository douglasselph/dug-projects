package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.FakeCards.Companion.fakeBloom
import dugsolutions.leaf.cards.FakeCards.Companion.fakeVine2
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.player.effect.AppliedEffect
import dugsolutions.leaf.player.effect.CardEffectProcessor
import dugsolutions.leaf.player.effect.EffectsList
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HandleDrawEffectTest {

    companion object {
        private const val CARD_ID_1 = 1
        private const val CARD_ID_2 = 2
        private const val DRAW_COUNT = 2

        private val EMPTY_EFFECTS_LIST = mockk<EffectsList> {
            every { iterator() } returns emptyList<AppliedEffect>().iterator()
            every { copy() } returns emptyList()
        }

        private val DRAW_CARDS_EFFECT = AppliedEffect.DrawCards(count = DRAW_COUNT, fromCompost = false)
        private val DRAW_CARDS_FROM_COMPOST_EFFECT = AppliedEffect.DrawCards(count = DRAW_COUNT, fromCompost = true)
        private val DRAW_DICE_EFFECT = AppliedEffect.DrawDice(count = DRAW_COUNT, fromCompost = false, drawHighest = false)
        private val DRAW_DICE_FROM_COMPOST_EFFECT = AppliedEffect.DrawDice(count = DRAW_COUNT, fromCompost = true, drawHighest = false)
        private val DRAW_BEST_DIE_EFFECT = AppliedEffect.DrawDice(count = DRAW_COUNT, fromCompost = false, drawHighest = true)
        private val DRAW_BEST_DIE_FROM_COMPOST_EFFECT = AppliedEffect.DrawDice(count = DRAW_COUNT, fromCompost = true, drawHighest = true)
        private val DRAW_THEN_DISCARD_EFFECT = AppliedEffect.DrawThenDiscard(drawCount = DRAW_COUNT, discardCount = 1)
        private val NON_DRAW_EFFECT = AppliedEffect.UpgradeDie()

        private val DRAW_CARDS_EFFECTS_LIST = mockk<EffectsList> {
            every { iterator() } returns listOf(DRAW_CARDS_EFFECT).iterator()
            every { copy() } returns listOf(DRAW_CARDS_EFFECT)
            every { remove(any()) } just Runs
            every { add(any()) } just Runs
        }

        private val DRAW_CARDS_FROM_COMPOST_EFFECTS_LIST = mockk<EffectsList> {
            every { iterator() } returns listOf(DRAW_CARDS_FROM_COMPOST_EFFECT).iterator()
            every { copy() } returns listOf(DRAW_CARDS_FROM_COMPOST_EFFECT)
            every { remove(any()) } just Runs
        }

        private val DRAW_DICE_EFFECTS_LIST = mockk<EffectsList> {
            every { iterator() } returns listOf(DRAW_DICE_EFFECT).iterator()
            every { copy() } returns listOf(DRAW_DICE_EFFECT)
            every { remove(any()) } just Runs
        }

        private val DRAW_DICE_FROM_COMPOST_EFFECTS_LIST = mockk<EffectsList> {
            every { iterator() } returns listOf(DRAW_DICE_FROM_COMPOST_EFFECT).iterator()
            every { copy() } returns listOf(DRAW_DICE_FROM_COMPOST_EFFECT)
            every { remove(any()) } just Runs
        }

        private val DRAW_BEST_DIE_EFFECTS_LIST = mockk<EffectsList> {
            every { iterator() } returns listOf(DRAW_BEST_DIE_EFFECT).iterator()
            every { copy() } returns listOf(DRAW_BEST_DIE_EFFECT)
            every { remove(any()) } just Runs
        }

        private val DRAW_BEST_DIE_FROM_COMPOST_EFFECTS_LIST = mockk<EffectsList> {
            every { iterator() } returns listOf(DRAW_BEST_DIE_FROM_COMPOST_EFFECT).iterator()
            every { copy() } returns listOf(DRAW_BEST_DIE_FROM_COMPOST_EFFECT)
            every { remove(any()) } just Runs
        }

        private val DRAW_THEN_DISCARD_EFFECTS_LIST = mockk<EffectsList> {
            every { iterator() } returns listOf(DRAW_THEN_DISCARD_EFFECT).iterator()
            every { copy() } returns listOf(DRAW_THEN_DISCARD_EFFECT)
            every { remove(any()) } just Runs
        }

        private val MULTIPLE_EFFECTS_LIST = mockk<EffectsList> {
            every { iterator() } returns listOf(DRAW_CARDS_EFFECT, DRAW_DICE_EFFECT).iterator()
            every { copy() } returns listOf(DRAW_CARDS_EFFECT, DRAW_DICE_EFFECT)
            every { remove(any()) } just Runs
        }
    }

    private lateinit var SUT: HandleDrawEffect
    private lateinit var mockDie: Die
    private lateinit var mockDie2: Die
    private lateinit var mockDie3: Die
    private lateinit var mockDie4: Die
    private lateinit var mockPlayer: Player
    private lateinit var fakePlayer: PlayerTD
    private lateinit var mockGameChronicle: GameChronicle
    private lateinit var mockCardManager: CardManager
    private lateinit var mockCardEffectProcessor: CardEffectProcessor

    @BeforeEach
    fun setup() {
        mockDie = mockk(relaxed = true)
        mockDie2 = mockk(relaxed = true)
        mockDie3 = mockk(relaxed = true)
        mockDie4 = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        mockGameChronicle = mockk(relaxed = true)
        mockCardManager = mockk(relaxed = true)
        mockCardEffectProcessor = mockk(relaxed = true)

        fakePlayer = PlayerTD(1, mockCardManager)

        SUT = HandleDrawEffect(mockCardEffectProcessor, mockCardManager, mockGameChronicle)

        every { mockPlayer.drawCard() } returns CARD_ID_1
        every { mockPlayer.drawCardFromCompost() } returns CARD_ID_2
        every { mockPlayer.drawDieFromCompost() } returns mockDie2
        every { mockPlayer.drawDie() } returns mockDie
        every { mockPlayer.drawBestDie() } returns mockDie3
        every { mockPlayer.drawBestDieFromCompost() } returns mockDie4
        every { mockGameChronicle(any()) } just Runs
        every { mockPlayer.effectsList } returns DRAW_CARDS_EFFECTS_LIST
    }

    @Test
    fun invoke_whenEmptyEffectsList_doesNothing() {
        // Arrange
        every { mockPlayer.effectsList } returns EMPTY_EFFECTS_LIST

        // Act
        SUT(mockPlayer)

        // Assert
        verify(exactly = 0) { mockPlayer.drawCard() }
        verify(exactly = 0) { mockPlayer.drawCardFromCompost() }
        verify(exactly = 0) { mockPlayer.drawDie() }
        verify(exactly = 0) { mockPlayer.drawDieFromCompost() }
        verify(exactly = 0) { mockPlayer.drawBestDie() }
        verify(exactly = 0) { mockPlayer.drawBestDieFromCompost() }
    }

    @Test
    fun invoke_whenDrawCardsEffect_drawsCards() {
        // Arrange
        every { mockPlayer.effectsList } returns DRAW_CARDS_EFFECTS_LIST

        // Act
        SUT(mockPlayer)

        // Assert
        repeat(DRAW_COUNT) {
            verify { mockPlayer.drawCard() }
        }
        verify(exactly = 0) { mockPlayer.drawCardFromCompost() }
        verify(exactly = 0) { mockPlayer.drawDie() }
        verify(exactly = 0) { mockPlayer.drawDieFromCompost() }
        verify(exactly = 0) { mockPlayer.drawBestDie() }
        verify(exactly = 0) { mockPlayer.drawBestDieFromCompost() }
    }

    @Test
    fun invoke_whenDrawCardsFromCompostEffect_drawsCardsFromCompost() {
        // Arrange
        every { mockPlayer.effectsList } returns DRAW_CARDS_FROM_COMPOST_EFFECTS_LIST

        // Act
        SUT(mockPlayer)

        // Assert
        repeat(DRAW_COUNT) {
            verify { mockPlayer.drawCardFromCompost() }
        }
        verify(exactly = 0) { mockPlayer.drawCard() }
        verify(exactly = 0) { mockPlayer.drawDie() }
        verify(exactly = 0) { mockPlayer.drawDieFromCompost() }
        verify(exactly = 0) { mockPlayer.drawBestDie() }
        verify(exactly = 0) { mockPlayer.drawBestDieFromCompost() }
    }

    @Test
    fun invoke_whenDrawDiceEffect_drawsDice() {
        // Arrange
        every { mockPlayer.effectsList } returns DRAW_DICE_EFFECTS_LIST

        // Act
        SUT(mockPlayer)

        // Assert
        repeat(DRAW_COUNT) {
            verify { mockPlayer.drawDie() }
        }
        verify(exactly = 0) { mockPlayer.drawCard() }
        verify(exactly = 0) { mockPlayer.drawCardFromCompost() }
        verify(exactly = 0) { mockPlayer.drawDieFromCompost() }
        verify(exactly = 0) { mockPlayer.drawBestDie() }
        verify(exactly = 0) { mockPlayer.drawBestDieFromCompost() }
    }

    @Test
    fun invoke_whenDrawDiceFromCompostEffect_drawsDiceFromCompost() {
        // Arrange
        every { mockPlayer.effectsList } returns DRAW_DICE_FROM_COMPOST_EFFECTS_LIST

        // Act
        SUT(mockPlayer)

        // Assert
        repeat(DRAW_COUNT) {
            verify { mockPlayer.drawDieFromCompost() }
        }
        verify(exactly = 0) { mockPlayer.drawCard() }
        verify(exactly = 0) { mockPlayer.drawCardFromCompost() }
        verify(exactly = 0) { mockPlayer.drawDie() }
        verify(exactly = 0) { mockPlayer.drawBestDie() }
        verify(exactly = 0) { mockPlayer.drawBestDieFromCompost() }
    }

    @Test
    fun invoke_whenDrawBestDieEffect_drawsBestDie() {
        // Arrange
        every { mockPlayer.effectsList } returns DRAW_BEST_DIE_EFFECTS_LIST

        // Act
        SUT(mockPlayer)

        // Assert
        repeat(DRAW_COUNT) {
            verify { mockPlayer.drawBestDie() }
        }
        verify(exactly = 0) { mockPlayer.drawCard() }
        verify(exactly = 0) { mockPlayer.drawCardFromCompost() }
        verify(exactly = 0) { mockPlayer.drawDie() }
        verify(exactly = 0) { mockPlayer.drawDieFromCompost() }
        verify(exactly = 0) { mockPlayer.drawBestDieFromCompost() }
    }

    @Test
    fun invoke_whenDrawBestDieFromCompostEffect_drawsBestDieFromCompost() {
        // Arrange
        every { mockPlayer.effectsList } returns DRAW_BEST_DIE_FROM_COMPOST_EFFECTS_LIST

        // Act
        SUT(mockPlayer)

        // Assert
        repeat(DRAW_COUNT) {
            verify { mockPlayer.drawBestDieFromCompost() }
        }
        verify(exactly = 0) { mockPlayer.drawCard() }
        verify(exactly = 0) { mockPlayer.drawCardFromCompost() }
        verify(exactly = 0) { mockPlayer.drawDie() }
        verify(exactly = 0) { mockPlayer.drawDieFromCompost() }
        verify(exactly = 0) { mockPlayer.drawBestDie() }
    }

    @Test
    fun invoke_whenDrawThenDiscardEffect_doesNothing() {
        // Arrange
        every { mockPlayer.effectsList } returns DRAW_THEN_DISCARD_EFFECTS_LIST

        // Act
        SUT(mockPlayer)

        // Assert
        verify(exactly = 0) { mockPlayer.drawCard() }
        verify(exactly = 0) { mockPlayer.drawCardFromCompost() }
        verify(exactly = 0) { mockPlayer.drawDie() }
        verify(exactly = 0) { mockPlayer.drawDieFromCompost() }
        verify(exactly = 0) { mockPlayer.drawBestDie() }
        verify(exactly = 0) { mockPlayer.drawBestDieFromCompost() }
    }

    @Test
    fun invoke_whenMultipleEffects_processesAllEffects() {
        // Arrange
        every { mockPlayer.effectsList } returns MULTIPLE_EFFECTS_LIST

        // Act
        SUT(mockPlayer)

        // Assert
        repeat(DRAW_COUNT) {
            verify { mockPlayer.drawCard() }
        }
        repeat(DRAW_COUNT) {
            verify { mockPlayer.drawDie() }
        }
        verify(exactly = 0) { mockPlayer.drawCardFromCompost() }
        verify(exactly = 0) { mockPlayer.drawDieFromCompost() }
        verify(exactly = 0) { mockPlayer.drawBestDie() }
        verify(exactly = 0) { mockPlayer.drawBestDieFromCompost() }
    }

    @Test
    fun invoke_whenDrawnCardHasNoEffects_doesNotTriggerRecursion() {
        // Arrange
        every { mockCardManager.getCard(CARD_ID_1) } returns null

        // Act
        SUT(mockPlayer)

        // Assert
        verify(exactly = DRAW_COUNT) { mockPlayer.drawCard() }
        verify(exactly = 0) { mockCardEffectProcessor.processCardEffect(any(), any()) }
    }

    @Test
    fun invoke_whenDrawnCardHasNonDrawEffects_addsToEffectsList() {
        // Arrange
        val fakeBloom = FakeCards.fakeBloom2
        every { mockCardManager.getCard(fakeBloom.id) } returns fakeBloom
        fakePlayer.addCardToSupply(fakeBloom)
        fakePlayer.addCardToHand(FakeCards.fakeVine)
        fakePlayer.effectsList.add(AppliedEffect.DrawCards(count = 1, fromCompost = false))

        // Act
        SUT(fakePlayer)

        // Assert
        assertEquals(fakeBloom, fakePlayer.cardsInHand.first { it == fakeBloom })
        verify(exactly = 1) { mockCardEffectProcessor.processCardEffect(fakeBloom, fakePlayer) }
        fakePlayer.effectsList.any { it is AppliedEffect.UseOpponent }
    }

    @Test
    fun invoke_whenDrawnCardHasDrawEffects_triggersRecursion() {
        // Arrange
        val fakeVine2 = FakeCards.fakeVine2
        val fakeBloom = FakeCards.fakeBloom3
        every { mockCardManager.getCard(fakeBloom.id) } returns fakeBloom
        every { mockCardManager.getCard(fakeVine2.id) } returns fakeVine2
        fakePlayer.addCardToSupply(fakeBloom)
        fakePlayer.addCardToSupply(fakeVine2)
        fakePlayer.addCardToHand(FakeCards.fakeVine)
        fakePlayer.effectsList.add(AppliedEffect.DrawCards(count = 1, fromCompost = false))
        every { mockCardEffectProcessor.processCardEffect(fakeBloom, fakePlayer) } returns listOf(
            AppliedEffect.DrawCards(
                count = 1,
                fromCompost = false
            )
        )

        // Act
        SUT(fakePlayer)

        // Assert
        val locateBloom = fakePlayer.cardsInHand.first { it == fakeBloom }
        val locateVine = fakePlayer.cardsInHand.first { it == fakeVine2 }
        assertEquals(fakeBloom, locateBloom)
        assertEquals(fakeVine2, locateVine)
        verify(exactly = 1) { mockCardEffectProcessor.processCardEffect(fakeBloom, fakePlayer) }
        verify(exactly = 1) { mockCardEffectProcessor.processCardEffect(fakeVine2, fakePlayer) }

        fakePlayer.effectsList.any { it is AppliedEffect.MarketBenefit }
    }

    @Test
    fun invoke_whenDrawnCardHasMixedEffects_handlesBothTypes() {
        // Arrange
        val fakeBloomDie = fakeBloom
        val fakeBloomCard = FakeCards.fakeBloom3
        every { mockCardManager.getCard(fakeBloomDie.id) } returns fakeBloom
        every { mockCardManager.getCard(fakeBloomCard.id) } returns fakeBloomCard
        every { mockCardEffectProcessor.processCardEffect(fakeBloomDie, fakePlayer) } returns listOf(
            AppliedEffect.DrawDice(
                count = 1,
                fromCompost = false
            )
        )
        fakePlayer.addCardToSupply(fakeBloomDie)
        fakePlayer.addCardToHand(fakeBloomCard)
        fakePlayer.addDieToSupply(mockDie)
        fakePlayer.effectsList.add(AppliedEffect.DrawCards(count = 1, fromCompost = false))

        // Act
        SUT(fakePlayer)

        // Assert
        val locateBloom = fakePlayer.cardsInHand.first { it == fakeBloomDie }
        val locateBloom2 = fakePlayer.cardsInHand.first { it == fakeBloomCard }
        assertEquals(fakeBloom, locateBloom)
        assertEquals(fakeBloomCard, locateBloom2)
        val locateDie = fakePlayer.diceInHand.dice.first { it == mockDie }
        assertEquals(mockDie, locateDie)
    }

}
