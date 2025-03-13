package dugsolutions.leaf.tool

interface Randomizer {
    fun nextBoolean(): Boolean
    fun nextInt(from: Int, until: Int): Int
    fun nextInt(until: Int): Int
    fun <T> randomOrNull(list: List<T>): T?
    fun <T> shuffled(list: List<T>): List<T>

    companion object {
        fun create(seed: Long? = null): Randomizer {
            return RandomizerDefault().apply { seed?.let { this.seed = it } }
        }
    }

}
