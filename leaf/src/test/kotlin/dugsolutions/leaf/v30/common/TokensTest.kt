package dugsolutions.leaf.v30.common

import dugsolutions.leaf.v30.random.die.DieSides
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TokensTest {

    @Test
    fun constructor_withNoArguments_startsEmpty() {
        val tokens = Tokens()

        assertEquals(0, tokens.waterCount)
        assertEquals(0, tokens.mulchCount)
        assertFalse(tokens.hasWater)
        assertFalse(tokens.hasMulch)
    }

    @Test
    fun constructor_withCounts_setsWaterAndMulchCounts() {
        val tokens = Tokens(
            waterCount = 2,
            mulchCounts = mapOf(
                DieSides.D4 to 1,
                DieSides.D8 to 3
            )
        )

        assertEquals(2, tokens.waterCount)
        assertEquals(4, tokens.mulchCount)
        assertEquals(1, tokens.getMulchCount(DieSides.D4))
        assertEquals(0, tokens.getMulchCount(DieSides.D6))
        assertEquals(3, tokens.getMulchCount(DieSides.D8))
        assertTrue(tokens.hasWater)
        assertTrue(tokens.hasMulch)
    }

    @Test
    fun has_withAvailableWater_returnsTrue() {
        val tokens = Tokens(waterCount = 1)

        assertTrue(tokens.has(Token.WATER))
    }

    @Test
    fun has_withAvailableMulchForMatchingSides_returnsTrue() {
        val tokens = Tokens(mulchCounts = mapOf(DieSides.D6 to 1))

        assertTrue(tokens.has(Token.MULCH(DieSides.D6)))
        assertFalse(tokens.has(Token.MULCH(DieSides.D8)))
    }


    @Test
    fun count_returnsCountForRequestedToken() {
        val tokens = Tokens(
            waterCount = 2,
            mulchCounts = mapOf(DieSides.D4 to 3)
        )

        assertEquals(2, tokens.count(Token.WATER))
        assertEquals(3, tokens.count(Token.MULCH(DieSides.D4)))
        assertEquals(0, tokens.count(Token.MULCH(DieSides.D6)))
    }

    @Test
    fun set_updatesOnlyRequestedTokenCountAndReturnsSameTokens() {
        val tokens = Tokens(
            waterCount = 2,
            mulchCounts = mapOf(DieSides.D4 to 3, DieSides.D6 to 1)
        )

        val result = tokens.set(Token.WATER, 5)
            .set(Token.MULCH(DieSides.D4), 7)

        assertSame(tokens, result)
        assertEquals(5, tokens.waterCount)
        assertEquals(7, tokens.getMulchCount(DieSides.D4))
        assertEquals(1, tokens.getMulchCount(DieSides.D6))
    }

    @Test
    fun set_withNegativeAmount_throwsException() {
        val tokens = Tokens()

        assertThrows<IllegalArgumentException> {
            tokens.set(Token.WATER, -1)
        }
    }

    @Test
    fun pull_withAvailableWater_decrementsAndReturnsWaterToken() {
        val tokens = Tokens(waterCount = 2)

        val result = tokens.pull(Token.WATER)

        assertSame(Token.WATER, result)
        assertEquals(1, tokens.waterCount)
        assertTrue(tokens.hasWater)
    }

    @Test
    fun pull_withLastWater_decrementsAndHasWaterBecomesFalse() {
        val tokens = Tokens(waterCount = 1)

        val result = tokens.pull(Token.WATER)

        assertSame(Token.WATER, result)
        assertEquals(0, tokens.waterCount)
        assertFalse(tokens.hasWater)
    }

    @Test
    fun pull_withNoWater_returnsNullAndLeavesCount() {
        val tokens = Tokens()

        val result = tokens.pull(Token.WATER)

        assertNull(result)
        assertEquals(0, tokens.waterCount)
    }

    @Test
    fun pull_withAvailableMulch_decrementsMatchingSidesOnlyAndReturnsToken() {
        val token = Token.MULCH(DieSides.D10)
        val tokens = Tokens(
            mulchCounts = mapOf(
                DieSides.D10 to 2,
                DieSides.D12 to 1
            )
        )

        val result = tokens.pull(token)

        assertEquals(token, result)
        assertEquals(1, tokens.getMulchCount(DieSides.D10))
        assertEquals(1, tokens.getMulchCount(DieSides.D12))
        assertEquals(2, tokens.mulchCount)
        assertTrue(tokens.hasMulch)
    }

    @Test
    fun pull_withNoMulchForMatchingSides_returnsNullAndLeavesCounts() {
        val tokens = Tokens(mulchCounts = mapOf(DieSides.D4 to 1))

        val result = tokens.pull(Token.MULCH(DieSides.D20))

        assertNull(result)
        assertEquals(1, tokens.getMulchCount(DieSides.D4))
        assertEquals(0, tokens.getMulchCount(DieSides.D20))
        assertEquals(1, tokens.mulchCount)
    }

    @Test
    fun pull_withLastMulch_decrementsAndHasMulchBecomesFalse() {
        val tokens = Tokens(mulchCounts = mapOf(DieSides.D6 to 1))

        val result = tokens.pull(Token.MULCH(DieSides.D6))

        assertEquals(Token.MULCH(DieSides.D6), result)
        assertEquals(0, tokens.getMulchCount(DieSides.D6))
        assertEquals(0, tokens.mulchCount)
        assertFalse(tokens.hasMulch)
    }

    @Test
    fun returnToken_withWater_incrementsWaterAndReturnsSameTokens() {
        val tokens = Tokens()

        val result = tokens.returnToken(Token.WATER)

        assertSame(tokens, result)
        assertEquals(1, tokens.waterCount)
        assertTrue(tokens.hasWater)
    }

    @Test
    fun returnToken_withMulch_incrementsMatchingMulchSides() {
        val tokens = Tokens(mulchCounts = mapOf(DieSides.D4 to 1))

        val result = tokens.returnToken(Token.MULCH(DieSides.D4))
            .returnToken(Token.MULCH(DieSides.D8))

        assertSame(tokens, result)
        assertEquals(2, tokens.getMulchCount(DieSides.D4))
        assertEquals(1, tokens.getMulchCount(DieSides.D8))
        assertEquals(3, tokens.mulchCount)
        assertTrue(tokens.hasMulch)
    }

    @Test
    fun reset_withNoArguments_clearsWaterAndMulchCounts() {
        val tokens = Tokens(
            waterCount = 2,
            mulchCounts = mapOf(DieSides.D4 to 1, DieSides.D6 to 3)
        )

        tokens.reset()

        assertEquals(0, tokens.waterCount)
        assertEquals(0, tokens.mulchCount)
        assertFalse(tokens.hasWater)
        assertFalse(tokens.hasMulch)
    }

    @Test
    fun reset_withCounts_replacesExistingCounts() {
        val tokens = Tokens(
            waterCount = 2,
            mulchCounts = mapOf(DieSides.D4 to 1, DieSides.D6 to 3)
        )

        tokens.reset(
            waterCount = 5,
            mulchCounts = mapOf(DieSides.D20 to 4)
        )

        assertEquals(5, tokens.waterCount)
        assertEquals(4, tokens.mulchCount)
        assertEquals(0, tokens.getMulchCount(DieSides.D4))
        assertEquals(0, tokens.getMulchCount(DieSides.D6))
        assertEquals(4, tokens.getMulchCount(DieSides.D20))
    }

    @Test
    fun constructorOrReset_withNegativeCounts_throwsException() {
        val tokens = Tokens()

        assertThrows<IllegalArgumentException> { Tokens(waterCount = -1) }
        assertThrows<IllegalArgumentException> { Tokens(mulchCounts = mapOf(DieSides.D4 to -1)) }
        assertThrows<IllegalArgumentException> { tokens.reset(waterCount = -1) }
        assertThrows<IllegalArgumentException> { tokens.reset(mulchCounts = mapOf(DieSides.D4 to -1)) }
    }

}
