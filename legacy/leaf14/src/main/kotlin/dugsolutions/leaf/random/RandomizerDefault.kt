package dugsolutions.leaf.random

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

    override fun nextBoolean(): Boolean {
        return random.nextBoolean()
    }

    override fun nextInt(from: Int, until: Int): Int {
        return random.nextInt(from, until)
    }

    override fun nextInt(until: Int): Int {
        return random.nextInt(until)
    }

    override fun <T> randomOrNull(list: List<T>): T? {
        return list.randomOrNull(random)
    }

    override fun <T> shuffled(list: List<T>): List<T> {
        return list.shuffled(random)
    }
}
