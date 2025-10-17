package dugsolutions.leaf.cards.domain

enum class CardEffect(
    val match: String,
    val description: String,
) {
    NONE("-", "No effect"),
    ADD_TO_DIE("AddToDie", "Add VALUE to one die, without exceeding MAX"),
    GRAFT_DIE("GraftDie", "Add a grafted die to the Canopy"),
    REROLL_ACCEPT_2ND("RerollAccept2nd", "Reroll VALUE dice, you must accept the second roll"),
    UPGRADE("Upgrade", "Upgrade VALUE dice then discard.");

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
