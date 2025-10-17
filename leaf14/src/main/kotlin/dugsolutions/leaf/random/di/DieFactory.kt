package dugsolutions.leaf.random.di

import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.Randomizer

class DieFactory(
    val randomizer: Randomizer
) {

    private val dieFactoryRandom = DieFactoryRandom(randomizer)
    private val dieFactoryUniform = DieFactoryUniform(randomizer)
    private var dieFactory: DieFactoryImpl = dieFactoryRandom

    // region public

    enum class Config {
        RANDOM,
        UNIFORM
    }

    var config: Config = Config.RANDOM
        set(value) {
            field = value
            dieFactory = when (field) {
                Config.RANDOM -> dieFactoryRandom
                Config.UNIFORM -> dieFactoryUniform
            }
        }

    val startingDice: List<Die>
        get() = listOf(
            invoke(DieSides.D4),
            invoke(DieSides.D4),
            invoke(DieSides.D6),
            invoke(DieSides.D6)
        )

    operator fun invoke(sides: DieSides): Die {
        return dieFactory(sides)
    }

    operator fun invoke(sides: Int): Die {
        return dieFactory(sides)
    }

    // endregion public


}
