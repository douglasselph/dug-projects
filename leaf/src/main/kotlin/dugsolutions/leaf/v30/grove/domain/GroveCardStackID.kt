package dugsolutions.leaf.v30.grove.domain

import dugsolutions.leaf.v30.cards.domain.CardType

enum class GroveCardStackID(
    val type: CardType,
    val cost: Int
) {
    ROOT_5(CardType.ROOT, 5),
    ROOT_7(CardType.ROOT, 7),
    ROOT_9(CardType.ROOT, 9),
    VINE_7(CardType.VINE, 7),
    VINE_9(CardType.VINE, 9),
    VINE_11(CardType.VINE, 11),
    FLOWER_11(CardType.FLOWER, 11),
    FLOWER_14(CardType.FLOWER, 14),
    FLOWER_17(CardType.FLOWER, 17);

    companion object {
        fun from(type: CardType, cost: Int): GroveCardStackID {
            return entries.firstOrNull { it.type == type && it.cost == cost }
                ?: throw IllegalArgumentException("No GroveStackID for type=$type cost=$cost")
        }
    }
}
