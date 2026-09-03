package dugsolutions.leaf.v35.tokens

class Tokens(
    waterCount: Int = 0,
    mulchTokens: List<Token.MULCH> = emptyList()
) {
    private var _waterCount = 0
    private val _mulchTokens = mutableListOf<Token.MULCH>()
    private val _pendingMulchTokens = mutableListOf<Token.PENDING_MULCH>()

    init {
        reset(waterCount, mulchTokens)
    }

    val waterCount: Int
        get() = _waterCount

    val mulchCount: Int
        get() = _mulchTokens.size

    val pendingMulchCount: Int
        get() = _pendingMulchTokens.size

    val mulchTokens: List<Token.MULCH>
        get() = _mulchTokens.toList()

    val hasWater: Boolean
        get() = waterCount > 0

    val hasMulch: Boolean
        get() = mulchCount > 0

    val hasPendingMulch: Boolean
        get() = pendingMulchCount > 0

    fun has(token: Token): Boolean {
        return when (token) {
            Token.WATER -> hasWater
            is Token.MULCH -> hasMulch
            is Token.PENDING_MULCH -> hasPendingMulch
        }
    }

    fun count(token: Token): Int {
        return when (token) {
            Token.WATER -> waterCount
            is Token.MULCH -> mulchCount
            is Token.PENDING_MULCH -> pendingMulchCount
        }
    }

    fun pull(token: Token): Token? {
        return when (token) {
            Token.WATER -> pullWater()
            is Token.MULCH -> pullMulch(token)
            is Token.PENDING_MULCH -> pullPendingMulch(token)
        }
    }

    fun add(token: Token): Tokens {
        when (token) {
            Token.WATER -> _waterCount++
            is Token.MULCH -> _mulchTokens.add(token)
            is Token.PENDING_MULCH -> _pendingMulchTokens.add(token)
        }
        return this
    }

    fun returnToken(token: Token): Tokens {
        return add(token)
    }

    fun set(token: Token, amount: Int): Tokens {
        require(amount >= 0) { "Token count cannot be negative: $amount" }
        when (token) {
            Token.WATER -> _waterCount = amount
            is Token.MULCH -> {
                _mulchTokens.clear()
                repeat(amount) {
                    _mulchTokens.add(token)
                }
            }
            else -> {}
        }
        return this
    }

    fun reset(
        waterCount: Int = 0,
        mulchTokens: List<Token.MULCH> = emptyList()
    ) {
        require(waterCount >= 0) { "Water token count cannot be negative: $waterCount" }

        _waterCount = waterCount
        _mulchTokens.clear()
        _mulchTokens.addAll(mulchTokens)
        _pendingMulchTokens.clear()
    }

    private fun pullWater(): Token? {
        if (!hasWater) return null
        _waterCount--
        return Token.WATER
    }

    private fun pullMulch(token: Token.MULCH): Token? {
        val index = _mulchTokens.indexOfFirst { it == token }
        if (index < 0) return null
        return _mulchTokens.removeAt(index)
    }

    private fun pullPendingMulch(token: Token.PENDING_MULCH): Token? {
        val index = _pendingMulchTokens.indexOfFirst { it == token }
        if (index < 0) return null
        return _pendingMulchTokens.removeAt(index)
    }

    fun normalize() {
        for (token in _pendingMulchTokens) {
            add(Token.MULCH(token.sides))
        }
        _pendingMulchTokens.clear()
    }

}
