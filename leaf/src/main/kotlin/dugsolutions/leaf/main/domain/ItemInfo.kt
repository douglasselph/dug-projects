package dugsolutions.leaf.main.domain

sealed class ItemInfo {

    data class Card(val value: CardInfo): ItemInfo()
    data class Die(val value: DieInfo): ItemInfo()

}
