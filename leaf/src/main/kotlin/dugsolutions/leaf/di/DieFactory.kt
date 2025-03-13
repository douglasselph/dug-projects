package dugsolutions.leaf.di

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides

interface DieFactory {

    operator fun invoke(sides: DieSides): Die
    operator fun invoke(sides: Int): Die

    val startingDice: List<Die>
        get() = listOf(
            invoke(DieSides.D4),
            invoke(DieSides.D4),
            invoke(DieSides.D6),
            invoke(DieSides.D6)
        )

}
