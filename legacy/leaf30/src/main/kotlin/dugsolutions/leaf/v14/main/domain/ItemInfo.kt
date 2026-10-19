package dugsolutions.leaf.v14.main.domain

sealed class ItemInfo {

    data class Card(val value: CardInfo): ItemInfo()
    data class Die(val value: DieInfo): ItemInfo()

}
