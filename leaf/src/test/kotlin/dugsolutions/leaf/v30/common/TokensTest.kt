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
        assertEquals(emptyList(), tokens.mulchTokens)
        assertFalse(tokens.hasWater)
        assertFalse(tokens.hasMulch)
    }

    @Test
    fun constructor_withTokens_setsWaterAndMulchTokens() {
        val mulch = listOf(Token.MULCH(), Token.MULCH(DieSides.D8))
        val tokens = Tokens(waterCount = 2, mulchTokens = mulch)

        assertEquals(2, tokens.waterCount)
        assertEquals(2, tokens.mulchCount)
        assertEquals(mulch, tokens.mulchTokens)
        assertTrue(tokens.hasWater)
        assertTrue(tokens.hasMulch)
    }

    @Test
    fun has_withAnyMulch_returnsTrueRegardlessOfRequestedSides() {
        val tokens = Tokens(mulchTokens = listOf(Token.MULCH(DieSides.D6)))

        assertTrue(tokens.has(Token.MULCH()))
        assertTrue(tokens.has(Token.MULCH(DieSides.D20)))
    }

    @Test
    fun count_withMulch_returnsAllMulchTokensRegardlessOfRequestedSides() {
        val tokens = Tokens(
            mulchTokens = listOf(
                Token.MULCH(),
                Token.MULCH(DieSides.D4),
                Token.MULCH(DieSides.D8)
            )
        )

        assertEquals(3, tokens.count(Token.MULCH()))
        assertEquals(3, tokens.count(Token.MULCH(DieSides.D4)))
        assertEquals(3, tokens.count(Token.MULCH(DieSides.D20)))
    }

    @Test
    fun set_withMulch_replacesMulchListWithExactTokenCopies() {
        val tokens = Tokens(mulchTokens = listOf(Token.MULCH(DieSides.D4)))

        val result = tokens.set(Token.MULCH(), 3)

        assertSame(tokens, result)
        assertEquals(3, tokens.mulchCount)
        assertEquals(listOf(Token.MULCH(), Token.MULCH(), Token.MULCH()), tokens.mulchTokens)
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
    fun pull_withNoWater_returnsNullAndLeavesCount() {
        val tokens = Tokens()

        val result = tokens.pull(Token.WATER)

        assertNull(result)
        assertEquals(0, tokens.waterCount)
    }

    @Test
    fun pull_withMulch_requiresExactNullSidesMatch() {
        val tokens = Tokens(mulchTokens = listOf(Token.MULCH(DieSides.D10)))

        val result = tokens.pull(Token.MULCH())

        assertNull(result)
        assertEquals(listOf(Token.MULCH(DieSides.D10)), tokens.mulchTokens)
    }

    @Test
    fun pull_withMulch_requiresExactNonNullSidesMatch() {
        val token = Token.MULCH(DieSides.D10)
        val tokens = Tokens(
            mulchTokens = listOf(
                Token.MULCH(),
                token,
                Token.MULCH(DieSides.D12)
            )
        )

        val result = tokens.pull(token)

        assertEquals(token, result)
        assertEquals(listOf(Token.MULCH(), Token.MULCH(DieSides.D12)), tokens.mulchTokens)
        assertEquals(2, tokens.mulchCount)
        assertTrue(tokens.hasMulch)
    }

    @Test
    fun pull_withLastMulch_decrementsAndHasMulchBecomesFalse() {
        val token = Token.MULCH(DieSides.D6)
        val tokens = Tokens(mulchTokens = listOf(token))

        val result = tokens.pull(token)

        assertEquals(token, result)
        assertEquals(emptyList(), tokens.mulchTokens)
        assertEquals(0, tokens.mulchCount)
        assertFalse(tokens.hasMulch)
    }

    @Test
    fun add_withWater_incrementsWaterAndReturnsSameTokens() {
        val tokens = Tokens()

        val result = tokens.add(Token.WATER)

        assertSame(tokens, result)
        assertEquals(1, tokens.waterCount)
        assertTrue(tokens.hasWater)
    }

    @Test
    fun add_withMulch_appendsExactToken() {
        val tokens = Tokens(mulchTokens = listOf(Token.MULCH()))

        val result = tokens.add(Token.MULCH(DieSides.D8))

        assertSame(tokens, result)
        assertEquals(listOf(Token.MULCH(), Token.MULCH(DieSides.D8)), tokens.mulchTokens)
        assertEquals(2, tokens.mulchCount)
    }

    @Test
    fun returnToken_delegatesToAdd() {
        val tokens = Tokens()

        val result = tokens.returnToken(Token.MULCH(DieSides.D4))

        assertSame(tokens, result)
        assertEquals(listOf(Token.MULCH(DieSides.D4)), tokens.mulchTokens)
    }

    @Test
    fun reset_withNoArguments_clearsWaterAndMulchTokens() {
        val tokens = Tokens(
            waterCount = 2,
            mulchTokens = listOf(Token.MULCH(), Token.MULCH(DieSides.D6))
        )

        tokens.reset()

        assertEquals(0, tokens.waterCount)
        assertEquals(0, tokens.mulchCount)
        assertEquals(emptyList(), tokens.mulchTokens)
        assertFalse(tokens.hasWater)
        assertFalse(tokens.hasMulch)
    }

    @Test
    fun reset_withTokens_replacesExistingTokens() {
        val tokens = Tokens(
            waterCount = 2,
            mulchTokens = listOf(Token.MULCH(), Token.MULCH(DieSides.D6))
        )

        tokens.reset(
            waterCount = 5,
            mulchTokens = listOf(Token.MULCH(DieSides.D20))
        )

        assertEquals(5, tokens.waterCount)
        assertEquals(1, tokens.mulchCount)
        assertEquals(listOf(Token.MULCH(DieSides.D20)), tokens.mulchTokens)
    }

    @Test
    fun constructorOrReset_withNegativeWaterCount_throwsException() {
        val tokens = Tokens()

        assertThrows<IllegalArgumentException> { Tokens(waterCount = -1) }
        assertThrows<IllegalArgumentException> { tokens.reset(waterCount = -1) }
    }
}
