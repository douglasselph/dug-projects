package dugsolutions.leaf.v35.plant.domain

enum class PlantType(val match: String) {
    ROOT("Root"),
    FLOWER("Flower"),
    VINE("Vine");

    companion object {
        fun from(incoming: String): PlantType? {
            for (entry in entries) {
                if (entry.match == incoming) {
                    return entry
                }
            }
            return null
        }
    }
}
