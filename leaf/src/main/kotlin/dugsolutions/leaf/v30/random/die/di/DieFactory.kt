package dugsolutions.leaf.v30.random.die.di

import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieSides

class DieFactory(
    val randomizer: Randomizer
) {

    private val dieFactoryRandom = DieFactoryRandom(randomizer)
    private val dieFactoryOneOfEachFaceBag = DieFactoryOneOfEachFaceBag(randomizer)
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
                Config.UNIFORM -> dieFactoryOneOfEachFaceBag
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
