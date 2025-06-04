package dugsolutions.leaf.di.factory

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.tool.Randomizer

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
