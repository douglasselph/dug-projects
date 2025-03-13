package dugsolutions.leaf.tool

import kotlin.random.Random


class RandomizerDefault : Randomizer {
    private var random: Random = Random.Default
    private var _seed: Long? = null

    var seed: Long?
        get() = _seed
        set(value) {
            _seed = value
            random = value?.let { Random(value) } ?: Random.Default
        }

    override fun nextBoolean(): Boolean = random.nextBoolean()

    override fun nextInt(from: Int, until: Int): Int = random.nextInt(from, until)

    override fun nextInt(until: Int): Int = random.nextInt(until)

    override fun <T> randomOrNull(list: List<T>): T? = list.randomOrNull(random)

    override fun <T> shuffled(list: List<T>): List<T> = list.shuffled(random)
}
