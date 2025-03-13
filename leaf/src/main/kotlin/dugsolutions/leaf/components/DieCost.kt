package dugsolutions.leaf.components

import dugsolutions.leaf.components.die.Die

class DieCost {

    operator fun invoke(sides: Int): Int {
        return sides
    }

    operator fun invoke(die: Die): Int {
        return die.sides
    }

}