package dugsolutions.leaf.game.purchase

import dugsolutions.leaf.components.FlourishType

class ManagePurchasedFloralTypes {

    private val purchasedFlourishTypes = mutableSetOf<FlourishType>()
    
    val list: List<FlourishType>
        get() = purchasedFlourishTypes.toList()

    fun add(type: FlourishType) {
        purchasedFlourishTypes.add(type)
    }

    fun has(type: FlourishType): Boolean {
        return purchasedFlourishTypes.contains(type)
    }

    fun clear() {
        purchasedFlourishTypes.clear()
    }
}
