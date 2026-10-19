package dugsolutions.leaf.player.components

import dugsolutions.leaf.common.domain.Butterfly

class ButterflyManager {

    private val butterflies = mutableListOf<Butterfly>()

    fun add(butterfly: Butterfly) = butterflies.add(butterfly)
    fun remove(butterfly: Butterfly) = butterflies.remove(butterfly)
    fun has(butterfly: Butterfly) = butterflies.contains(butterfly)

}
