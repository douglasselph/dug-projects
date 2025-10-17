package dugsolutions.leaf.common.domain

enum class Effect(
    val match: String,
    val description: String,
) {
    NONE("-", "No effect"),
    ADD_TO_DIE("AddToDie", "Add VALUE to one die, without exceeding MAX"),
    GRAFT_DIE("GraftDie", "Add a grafted die to the Canopy"),
    REROLL_ACCEPT_2ND("RerollAccept2nd", "Reroll VALUE dice, you must accept the second roll"),
    REROLL_TAKE_BETTER("RerollTakeBetter", "Reroll VALUE dice, you may take the better of the two rolls"),
    UPGRADE("Upgrade", "Upgrade VALUE dice then discard.");

    companion object {
        fun from(incoming: String): Effect? {
            for (entry in entries) {
                if (entry.match == incoming) {
                    return entry
                }
            }
            return null
        }
    }
} 
