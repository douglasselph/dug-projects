package dugsolutions.leaf.random.die

import dugsolutions.leaf.random.di.DieFactoryRandom
import dugsolutions.leaf.random.Randomizer

class SampleDie(
    randomizer: Randomizer = Randomizer.create()
) {

    private val dieFactory = DieFactoryRandom(randomizer)

    val d4: Die
        get() = dieFactory(DieSides.D4).roll()
    val d6: Die
        get() = dieFactory(DieSides.D6).roll()
    val d8: Die
        get() = dieFactory(DieSides.D8).roll()
    val d10: Die
        get() = dieFactory(DieSides.D10).roll()
    val d12: Die
        get() = dieFactory(DieSides.D12).roll()
    val d20: Die
        get() = dieFactory(DieSides.D20).roll()

    val twoD6: List<Die>
        get() = listOf(d6, d6)
    val threeD6: List<Die>
        get() = listOf(d6, d6, d6)

    val mixedDice: List<Die>
        get() = listOf(d4, d6, d8)

    val randomDice: List<Die>
        get() = listOf(d4, d6, d8, d10)
}
