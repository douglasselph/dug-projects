package dugsolutions.leaf.v30.common

import dugsolutions.leaf.v30.random.die.DieSides

class Tokens(
    waterCount: Int = 0,
    mulchCounts: Map<DieSides, Int> = emptyMap()
) {
    private var _waterCount = 0
    private val mulchCounts = DieSides.entries.associateWith { 0 }.toMutableMap()

    init {
        reset(waterCount, mulchCounts)
    }

    val waterCount: Int
        get() = _waterCount

    val mulchCount: Int
        get() = mulchCounts.values.sum()

    val hasWater: Boolean
        get() = waterCount > 0

    val hasMulch: Boolean
        get() = mulchCount > 0

    fun getMulchCount(sides: DieSides): Int {
        return mulchCounts[sides] ?: 0
    }

    fun has(token: Token): Boolean {
        return when (token) {
            Token.WATER -> hasWater
            is Token.MULCH -> getMulchCount(token.sides) > 0
        }
    }

    fun count(token: Token): Int {
        return when (token) {
            Token.WATER -> waterCount
            is Token.MULCH -> getMulchCount(token.sides)
        }
    }

    fun pull(token: Token): Token? {
        if (!has(token)) return null
        when (token) {
            Token.WATER -> _waterCount--
            is Token.MULCH -> mulchCounts[token.sides] = getMulchCount(token.sides) - 1
        }
        return token
    }

    fun returnToken(token: Token): Tokens {
        when (token) {
            Token.WATER -> _waterCount++
            is Token.MULCH -> mulchCounts[token.sides] = getMulchCount(token.sides) + 1
        }
        return this
    }

    fun set(token: Token, amount: Int): Tokens {
        require(amount >= 0) { "Token count cannot be negative: $amount" }
        when (token) {
            Token.WATER -> _waterCount = amount
            is Token.MULCH -> mulchCounts[token.sides] = amount
        }
        return this
    }

    fun reset(
        waterCount: Int = 0,
        mulchCounts: Map<DieSides, Int> = emptyMap()
    ) {
        require(waterCount >= 0) { "Water token count cannot be negative: $waterCount" }
        mulchCounts.forEach { (sides, count) ->
            require(count >= 0) { "Mulch token count cannot be negative for $sides: $count" }
        }

        _waterCount = waterCount
        this.mulchCounts.keys.forEach { sides ->
            this.mulchCounts[sides] = mulchCounts[sides] ?: 0
        }
    }
}
