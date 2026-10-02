package dugsolutions.leaf.v35.tokens

class Tokens(
    waterCount: Int = 0,
    sunlightCount: Int = 0,
    mulchTokens: List<Token.MULCH> = emptyList(),
    private val supplyObserver: SharedTokenSupplyObserver = SharedTokenSupplyObserver.NONE
) {
    private var _waterCount = 0
    private var _sunlightCount = 0
    private val _mulchTokens = mutableListOf<Token.MULCH>()
    private val _pendingMulchTokens = mutableListOf<Token.PENDING_MULCH>()

    init {
        reset(waterCount, sunlightCount, mulchTokens)
    }

    val waterCount: Int
        get() = _waterCount

    val sunlightCount: Int
        get() = _sunlightCount

    val mulchCount: Int
        get() = _mulchTokens.size

    val pendingMulchCount: Int
        get() = _pendingMulchTokens.size

    val mulchTokens: List<Token.MULCH>
        get() = _mulchTokens.toList()

    val pendingMulchTokens: List<Token.PENDING_MULCH>
        get() = _pendingMulchTokens.toList()

    val hasWater: Boolean
        get() = waterCount > 0

    val hasSunlight: Boolean
        get() = sunlightCount > 0

    val hasMulch: Boolean
        get() = mulchCount > 0

    val hasPendingMulch: Boolean
        get() = pendingMulchCount > 0

    fun has(token: Token): Boolean {
        return when (token) {
            Token.WATER -> hasWater
            Token.SUNLIGHT -> hasSunlight
            is Token.MULCH -> hasMulch
            is Token.PENDING_MULCH -> hasPendingMulch
        }
    }

    fun count(token: Token): Int {
        return when (token) {
            Token.WATER -> waterCount
            Token.SUNLIGHT -> sunlightCount
            is Token.MULCH -> mulchCount
            is Token.PENDING_MULCH -> pendingMulchCount
        }
    }

    fun pull(token: Token): Token? {
        return when (token) {
            Token.WATER -> pullWater()
            Token.SUNLIGHT -> pullSunlight()
            is Token.MULCH -> pullMulch(token)
            is Token.PENDING_MULCH -> pullPendingMulch(token)
        }
    }

    fun add(token: Token): Tokens {
        when (token) {
            Token.WATER -> {
                supplyObserver.onReturn(SharedTokenResource.WATER)
                _waterCount++
            }
            Token.SUNLIGHT -> {
                supplyObserver.onReturn(SharedTokenResource.SUNLIGHT)
                _sunlightCount++
            }
            is Token.MULCH -> {
                supplyObserver.onReturn(SharedTokenResource.MULCH)
                _mulchTokens.add(token)
            }
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
            Token.SUNLIGHT -> _sunlightCount = amount
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
        sunlightCount: Int = 0,
        mulchTokens: List<Token.MULCH> = emptyList()
    ) {
        require(waterCount >= 0) { "Water token count cannot be negative: $waterCount" }
        require(sunlightCount >= 0) { "Sunlight token count cannot be negative: $sunlightCount" }

        _waterCount = waterCount
        _sunlightCount = sunlightCount
        _mulchTokens.clear()
        _mulchTokens.addAll(mulchTokens)
        _pendingMulchTokens.clear()
    }

    private fun pullWater(): Token? {
        val success = hasWater
        supplyObserver.onGainAttempt(SharedTokenResource.WATER, success)
        if (!success) return null
        _waterCount--
        return Token.WATER
    }

    private fun pullSunlight(): Token? {
        val success = hasSunlight
        supplyObserver.onGainAttempt(SharedTokenResource.SUNLIGHT, success)
        if (!success) return null
        _sunlightCount--
        return Token.SUNLIGHT
    }

    private fun pullMulch(token: Token.MULCH): Token? {
        val index = _mulchTokens.indexOfFirst { it == token }
        val success = index >= 0
        supplyObserver.onGainAttempt(SharedTokenResource.MULCH, success)
        if (!success) return null
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
