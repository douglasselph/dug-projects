package dugsolutions.leaf.cards.domain

enum class FlourishType(private val match: String) {
    NONE(""),
    CANOPY("Canopy"),
    RESOURCE("Resource"),
    ROOT("Root"),
    WildVINE("WildVine"),
    Vine("Vine"),
    FLOWER("Flower"),
    BUTTERFLY("Butterfly"),
    WISP("Wisp");

    companion object {
        fun from(incoming: String): FlourishType {
            for (entry in entries) {
                if (incoming.startsWith(entry.match, ignoreCase = true)) {
                    return entry
                }
            }
            throw IllegalArgumentException("No matching FlourishType found for: $incoming")
        }
    }
}
