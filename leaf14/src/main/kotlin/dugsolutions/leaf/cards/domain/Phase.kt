package dugsolutions.leaf.cards.domain

enum class Phase(private val match: String) {

    Cultivation("C"),
    Battle("B");

    companion object {
        fun from(incoming: String): Phase {
            for (entry in entries) {
                if (incoming.startsWith(entry.match, ignoreCase = true)) {
                    return entry
                }
            }
            throw IllegalArgumentException("No matching Phase found for: $incoming")
        }
    }

}
