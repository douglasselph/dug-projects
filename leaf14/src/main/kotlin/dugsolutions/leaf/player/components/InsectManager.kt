package dugsolutions.leaf.player.components

import dugsolutions.leaf.common.domain.Token

class InsectManager {

    private val insects = mutableListOf<Token>()

    fun add(insect: Token) = insects.add(insect)
    fun add(insect: Token, count: Int) = repeat(count) { add(insect) }
    fun remove(insect : Token) = insects.remove(insect)

    val all: List<Token>
        get() = insects.toList()

    fun countOf(insect: Token): Int =  insects.filter { it == insect }.size

}
