package dugsolutions.leaf.v14.game.acquire

import dugsolutions.leaf.v14.cards.domain.FlourishType

class ManageAcquiredFloralTypes {

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
