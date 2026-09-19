package dugsolutions.leaf.v30.cards.domain

enum class CardType(val match: String) {
    ROOT("Root"),
    FLOWER("Flower"),
    VINE("Vine");

    companion object {
        fun from(incoming: String): CardType? {
            for (entry in entries) {
                if (entry.match == incoming) {
                    return entry
                }
            }
            return null
        }
    }
}
