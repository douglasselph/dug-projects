package dugsolutions.leaf.v35.tokens

import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TokensTest {

    @Test
    fun constructor_initializesWaterAndMulch() {
        // Arrange
        val mulch = listOf(
            Token.MULCH(),
            Token.MULCH()
        )

        // Act
        val result = Tokens(
            waterCount = 3,
            mulchTokens = mulch
        )

        // Assert
        assertEquals(3, result.waterCount)
        assertEquals(2, result.mulchCount)
        assertEquals(0, result.pendingMulchCount)
        assertEquals(mulch, result.mulchTokens)
        assertTrue(result.hasWater)
        assertTrue(result.hasMulch)
        assertFalse(result.hasPendingMulch)
    }

    @Test
    fun constructor_whenWaterCountNegative_throws() {
        // Act / Assert
        assertFailsWith<IllegalArgumentException> {
            Tokens(waterCount = -1)
        }
    }

    @Test
    fun hasAndCount_returnValuesForEachTokenState() {
        // Arrange
        val tokens = Tokens(waterCount = 2)
            .add(Token.MULCH())
            .add(Token.PENDING_MULCH())

        // Act / Assert
        assertTrue(tokens.has(Token.WATER))
        assertEquals(2, tokens.count(Token.WATER))

        assertTrue(tokens.has(Token.MULCH()))
        assertEquals(1, tokens.count(Token.MULCH()))

        assertTrue(tokens.has(Token.PENDING_MULCH()))
        assertEquals(1, tokens.count(Token.PENDING_MULCH()))
    }

    @Test
    fun pendingMulchTokens_exposesDefensiveSnapshotWithStoredDieSides() {
        val tokens = Tokens()
            .add(Token.PENDING_MULCH(DieSides.D10))

        val snapshot = tokens.pendingMulchTokens
        tokens.add(Token.PENDING_MULCH(DieSides.D6))

        assertEquals(
            listOf(Token.PENDING_MULCH(DieSides.D10)),
            snapshot
        )
        assertEquals(2, tokens.pendingMulchTokens.size)
    }

    @Test
    fun add_pendingMulch_tracksItSeparatelyFromAcquiredMulch() {
        // Arrange
        val tokens = Tokens()
        val pending = Token.PENDING_MULCH()

        // Act
        tokens.add(pending)

        // Assert
        assertEquals(0, tokens.mulchCount)
        assertEquals(1, tokens.pendingMulchCount)
        assertFalse(tokens.hasMulch)
        assertTrue(tokens.hasPendingMulch)
    }

    @Test
    fun add_addsTokenAndReturnsSameCollection() {
        // Arrange
        val tokens = Tokens()

        // Act
        val returned = tokens
            .add(Token.WATER)
            .add(Token.MULCH())
            .add(Token.PENDING_MULCH())

        // Assert
        assertTrue(returned === tokens)
        assertEquals(1, tokens.waterCount)
        assertEquals(1, tokens.mulchCount)
        assertEquals(1, tokens.pendingMulchCount)
    }

    @Test
    fun returnToken_addsTokenBack() {
        // Arrange
        val tokens = Tokens(waterCount = 1)
        val pulled = tokens.pull(Token.WATER)

        // Act
        val returned = tokens.returnToken(pulled!!)

        // Assert
        assertTrue(returned === tokens)
        assertEquals(1, tokens.waterCount)
    }

    @Test
    fun pullWater_whenAvailable_returnsWaterAndDecrementsCount() {
        // Arrange
        val tokens = Tokens(waterCount = 2)

        // Act
        val result = tokens.pull(Token.WATER)

        // Assert
        assertEquals(Token.WATER, result)
        assertEquals(1, tokens.waterCount)
    }

    @Test
    fun pullWater_whenUnavailable_returnsNull() {
        // Arrange
        val tokens = Tokens()

        // Act
        val result = tokens.pull(Token.WATER)

        // Assert
        assertNull(result)
        assertEquals(0, tokens.waterCount)
    }

    @Test
    fun pullMulch_whenMatchingTokenExists_removesAndReturnsIt() {
        // Arrange
        val mulch = Token.MULCH()
        val tokens = Tokens(
            mulchTokens = listOf(mulch, Token.MULCH())
        )

        // Act
        val result = tokens.pull(mulch)

        // Assert
        assertEquals(mulch, result)
        assertEquals(1, tokens.mulchCount)
    }

    @Test
    fun pullMulch_whenUnavailable_returnsNull() {
        // Arrange
        val tokens = Tokens()

        // Act
        val result = tokens.pull(Token.MULCH())

        // Assert
        assertNull(result)
    }

    @Test
    fun pullPendingMulch_whenAvailable_removesAndReturnsIt() {
        // Arrange
        val pending = Token.PENDING_MULCH()
        val tokens = Tokens().add(pending)

        // Act
        val result = tokens.pull(pending)

        // Assert
        assertEquals(pending, result)
        assertEquals(0, tokens.pendingMulchCount)
        assertFalse(tokens.hasPendingMulch)
    }

    @Test
    fun setWater_replacesWaterCount() {
        // Arrange
        val tokens = Tokens(waterCount = 2)

        // Act
        val returned = tokens.set(Token.WATER, 5)

        // Assert
        assertTrue(returned === tokens)
        assertEquals(5, tokens.waterCount)
    }

    @Test
    fun setMulch_replacesMulchCount() {
        // Arrange
        val mulch = Token.MULCH()
        val tokens = Tokens(
            mulchTokens = listOf(
                Token.MULCH(),
                Token.MULCH()
            )
        )

        // Act
        tokens.set(mulch, 3)

        // Assert
        assertEquals(3, tokens.mulchCount)
        assertEquals(
            listOf(mulch, mulch, mulch),
            tokens.mulchTokens
        )
    }

    @Test
    fun set_whenAmountNegative_throws() {
        // Arrange
        val tokens = Tokens()

        // Act / Assert
        assertFailsWith<IllegalArgumentException> {
            tokens.set(Token.WATER, -1)
        }
    }

    @Test
    fun reset_replacesWaterAndMulchAndClearsPendingMulch() {
        // Arrange
        val tokens = Tokens(waterCount = 5)
            .add(Token.MULCH())
            .add(Token.PENDING_MULCH())

        val replacementMulch = listOf(
            Token.MULCH(),
            Token.MULCH()
        )

        // Act
        tokens.reset(
            waterCount = 2,
            mulchTokens = replacementMulch
        )

        // Assert
        assertEquals(2, tokens.waterCount)
        assertEquals(replacementMulch, tokens.mulchTokens)
        assertEquals(0, tokens.pendingMulchCount)
    }

    @Test
    fun normalize_convertsPendingMulchToAcquiredMulch() {
        // Arrange
        val tokens = Tokens()
            .add(Token.PENDING_MULCH())
            .add(Token.PENDING_MULCH())

        // Act
        tokens.normalize()

        // Assert
        assertEquals(2, tokens.mulchCount)
        assertEquals(0, tokens.pendingMulchCount)
        assertTrue(tokens.hasMulch)
        assertFalse(tokens.hasPendingMulch)
    }

    @Test
    fun normalize_preservesMulchDieSides() {
        // Arrange
        val pending = Token.PENDING_MULCH(DieSides.D10)
        val tokens = Tokens().add(pending)

        // Act
        tokens.normalize()

        // Assert
        assertEquals(
            listOf(Token.MULCH(DieSides.D10)),
            tokens.mulchTokens
        )
        assertEquals(0, tokens.pendingMulchCount)
    }

    @Test
    fun normalize_addsResolvedMulchToExistingMulch() {
        // Arrange
        val existing = Token.MULCH()
        val tokens = Tokens(
            mulchTokens = listOf(existing)
        )
            .add(Token.PENDING_MULCH())
            .add(Token.PENDING_MULCH())

        // Act
        tokens.normalize()

        // Assert
        assertEquals(3, tokens.mulchCount)
        assertEquals(0, tokens.pendingMulchCount)
    }

    @Test
    fun normalize_whenNoPendingMulch_leavesExistingTokensUnchanged() {
        // Arrange
        val mulch = listOf(
            Token.MULCH(),
            Token.MULCH()
        )
        val tokens = Tokens(
            waterCount = 2,
            mulchTokens = mulch
        )

        // Act
        tokens.normalize()

        // Assert
        assertEquals(2, tokens.waterCount)
        assertEquals(mulch, tokens.mulchTokens)
        assertEquals(0, tokens.pendingMulchCount)
    }
}
