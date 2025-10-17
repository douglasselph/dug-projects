package dugsolutions.leaf.cards.domain

enum class MatchWith(val match: String) {
    None(""),
    PulledGraft("PulledGraft"),
    WormOrSap("Worm|Sap"),
    Sap("Sap"),
    Bee("Bee"),
    End("End");

    companion object {
        fun from(incoming: String): MatchWith {
            for (entry in entries) {
                if (incoming.startsWith(entry.match, ignoreCase = true)) {
                    return entry
                }
            }
            throw IllegalArgumentException("No matching MatchWith found for: $incoming")
        }
    }
}
