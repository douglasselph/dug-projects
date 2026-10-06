package dugsolutions.leaf.v35.tokens

/** Immutable starting counts for the shared Grove token supplies. */
data class SharedTokenStartingSupply(
    val water: Int = 9,
    val sunlight: Int = 9,
    val mulch: Int = 9,
    val bee: Int = 9,
    val worm: Int = 9
) {
    init {
        require(water >= 0) { "Water starting supply cannot be negative: $water" }
        require(sunlight >= 0) { "Sunlight starting supply cannot be negative: $sunlight" }
        require(mulch >= 0) { "Mulch starting supply cannot be negative: $mulch" }
        require(bee >= 0) { "Bee starting supply cannot be negative: $bee" }
        require(worm >= 0) { "Worm starting supply cannot be negative: $worm" }
    }

    companion object {
        val CANONICAL = SharedTokenStartingSupply()
    }
}
