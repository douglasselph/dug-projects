package dugsolutions.leaf.player.components

import dugsolutions.leaf.common.domain.Insect

class InsectManager {

    private val insects = mutableListOf<Insect>()

    fun add(insect: Insect) = insects.add(insect)
    fun remove(insect : Insect) = insects.remove(insect)

    val all: List<Insect>
        get() = insects.toList()

    fun countOf(insect: Insect): Int =  insects.filter { it == insect }.size

}
