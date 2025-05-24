package dugsolutions.leaf.game.acquire.credit

import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.game.acquire.domain.Credit
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.AppliedEffect
import dugsolutions.leaf.player.effect.EffectsList
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EffectToCreditsTest {

    private lateinit var mockPlayer: Player
    private lateinit var sampleDie: SampleDie
    private lateinit var SUT: EffectToCredits

    @BeforeEach
    fun setup() {
        mockPlayer = mockk(relaxed = true)
        sampleDie = SampleDie()

        SUT = EffectToCredits()
    }

    @Test
    fun invoke_withEmptyDiceAndEffects_returnsEmptyCredits() {
        // Arrange
        val emptyDice = Dice()
        every { mockPlayer.diceInHand } returns emptyDice
        every { mockPlayer.effectsList } returns EffectsList()

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertTrue(result.list.isEmpty(), "Should return empty credits list")
    }

    @Test
    fun invoke_withOnlyDiceInHand_returnsCorrectCredits() {
        // Arrange
        val die1 = sampleDie.d6
        val die2 = sampleDie.d8
        val dice = Dice(listOf(die1, die2))
        every { mockPlayer.diceInHand } returns dice
        every { mockPlayer.effectsList } returns EffectsList()

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(2, result.list.size)
        
        val credDie1 = result.list[0] as Credit.CredDie
        val credDie2 = result.list[1] as Credit.CredDie
        
        assertEquals(die1, credDie1.die)
        assertEquals(die2, credDie2.die)
    }

    @Test
    fun invoke_withAdjustDieRollEffect_returnsCorrectCredits() {
        // Arrange
        every { mockPlayer.diceInHand } returns Dice()
        val adjustment = 2
        val effectsList = EffectsList().apply {
            addAll(listOf(AppliedEffect.AdjustDieRoll(adjustment)))
        }
        every { mockPlayer.effectsList } returns effectsList

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(1, result.list.size)
        val credit = result.list[0] as Credit.CredAdjustDie
        assertEquals(adjustment, credit.value)
    }

    @Test
    fun invoke_withAdjustDieToMaxEffect_returnsSetToMaxCredit() {
        // Arrange
        every { mockPlayer.diceInHand } returns Dice()
        val effectsList = EffectsList().apply {
            addAll(listOf(AppliedEffect.AdjustDieToMax()))
        }
        every { mockPlayer.effectsList } returns effectsList

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(1, result.list.size)
        assertTrue(result.list[0] is Credit.CredSetToMax)
    }

    @Test
    fun invoke_withAddToTotalEffect_returnsCorrectCredits() {
        // Arrange
        every { mockPlayer.diceInHand } returns Dice()
        val amount = 3
        val effectsList = EffectsList().apply {
            addAll(listOf(AppliedEffect.AddToTotal(amount)))
        }
        every { mockPlayer.effectsList } returns effectsList

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(1, result.list.size)
        val credit = result.list[0] as Credit.CredAddToTotal
        assertEquals(amount, credit.amount)
    }

    @Test
    fun invoke_withRerollDieEffect_returnsRerollDieCredit() {
        // Arrange
        every { mockPlayer.diceInHand } returns Dice()
        val effectsList = EffectsList().apply {
            addAll(listOf(AppliedEffect.RerollDie(1)))
        }
        every { mockPlayer.effectsList } returns effectsList

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(1, result.list.size)
        assertTrue(result.list[0] is Credit.CredRerollDie)
    }

    @Test
    fun invoke_withMarketBenefitEffect_returnsCorrectCredits() {
        // Arrange
        every { mockPlayer.diceInHand } returns Dice()
        val type = FlourishType.ROOT
        val costReduction = 2
        val effectsList = EffectsList().apply {
            addAll(listOf(AppliedEffect.MarketBenefit(type, costReduction)))
        }
        every { mockPlayer.effectsList } returns effectsList

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(1, result.list.size)
        val credit = result.list[0] as Credit.CredReduceCost
        assertEquals(type, credit.type)
        assertEquals(costReduction, credit.amount)
    }

    @Test
    fun invoke_withMarketBenefitEffectNullType_returnsNoCredits() {
        // Arrange
        every { mockPlayer.diceInHand } returns Dice()
        val costReduction = 2
        val effectsList = EffectsList().apply {
            addAll(listOf(AppliedEffect.MarketBenefit(null, costReduction)))
        }
        every { mockPlayer.effectsList } returns effectsList

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertTrue(result.list.isEmpty())
    }

    @Test
    fun invoke_withUnsupportedEffect_ignoresEffect() {
        // Arrange
        every { mockPlayer.diceInHand } returns Dice()
        val effect = AppliedEffect.MarketBenefit()
        val effectsList = EffectsList().apply {
            addAll(listOf(effect))
        }
        every { mockPlayer.effectsList } returns effectsList

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertTrue(result.list.isEmpty())
    }

    @Test
    fun invoke_withMultipleDiceAndEffects_returnsAllCredits() {
        // Arrange
        val dice = Dice(sampleDie.mixedDice.take(1)) // Use the first die from the mixed dice collection
        every { mockPlayer.diceInHand } returns dice
        
        val adjustment = 2
        val addToTotal = 3
        val type = FlourishType.BLOOM
        val costReduction = 1
        
        val effectsList = EffectsList().apply {
            addAll(listOf(
                AppliedEffect.AdjustDieRoll(adjustment),
                AppliedEffect.AddToTotal(addToTotal),
                AppliedEffect.MarketBenefit(type, costReduction)
            ))
        }
        
        every { mockPlayer.effectsList } returns effectsList

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(4, result.list.size) // 1 die + 3 effects
        
        // Check die credit exists
        assertTrue(result.list.any { it is Credit.CredDie })
        
        // Check effect credits
        assertTrue(result.list.any { it is Credit.CredAdjustDie && it.value == adjustment })
        assertTrue(result.list.any { it is Credit.CredAddToTotal && it.amount == addToTotal })
        assertTrue(result.list.any { 
            it is Credit.CredReduceCost && 
            it.type == type && 
            it.amount == costReduction 
        })
    }
    
    @Test
    fun invoke_withMultipleDice_returnsCorrectCredits() {
        // Arrange
        val dice = Dice(sampleDie.twoD6) // Use the two d6 dice collection
        every { mockPlayer.diceInHand } returns dice
        every { mockPlayer.effectsList } returns EffectsList()

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(2, result.list.size)
        result.list.forEach { credit ->
            assertTrue(credit is Credit.CredDie)
        }
    }
    
    @Test
    fun handleEffect_withDifferentEffectTypes_mapsCorrectly() {
        // Testing the private handleEffect method through the public invoke method
        
        // Arrange
        every { mockPlayer.diceInHand } returns Dice()
        
        val adjustDieRoll = AppliedEffect.AdjustDieRoll(2)
        val adjustDieToMax = AppliedEffect.AdjustDieToMax()
        val addToTotal = AppliedEffect.AddToTotal(3)
        val rerollDie = AppliedEffect.RerollDie(1)
        val marketBenefit = AppliedEffect.MarketBenefit(FlourishType.VINE, 1)
        val unsupportedEffect = AppliedEffect.RetainCard
        
        val effectsList = EffectsList().apply {
            addAll(listOf(
                adjustDieRoll,
                adjustDieToMax,
                addToTotal,
                rerollDie,
                marketBenefit,
                unsupportedEffect
            ))
        }
        
        every { mockPlayer.effectsList } returns effectsList

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(5, result.list.size) // 5 supported effects
        
        // Verify each effect type was mapped correctly
        assertTrue(result.list.any { it is Credit.CredAdjustDie })
        assertTrue(result.list.any { it is Credit.CredSetToMax })
        assertTrue(result.list.any { it is Credit.CredAddToTotal })
        assertTrue(result.list.any { it is Credit.CredRerollDie })
        assertTrue(result.list.any { it is Credit.CredReduceCost })
    }
} 
