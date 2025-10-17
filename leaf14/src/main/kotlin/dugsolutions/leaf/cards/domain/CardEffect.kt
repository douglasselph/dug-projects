package dugsolutions.leaf.cards.domain

enum class CardEffect(
    val match: String,
    val description: String,
) {
    NONE("None", "This card has no special effect");

    companion object {
        fun from(incoming: String): CardEffect? {
            for (entry in entries) {
                if (entry.match == incoming) {
                    return entry
                }
            }
            return null
        }
    }
} 
