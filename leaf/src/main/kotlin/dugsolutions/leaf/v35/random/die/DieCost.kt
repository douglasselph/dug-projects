package dugsolutions.leaf.v35.random.die

import dugsolutions.leaf.v14.random.die.Die

class DieCost {

    operator fun invoke(sides: Int): Int {
        return sides
    }

    operator fun invoke(die: Die): Int {
        return die.sides
    }

}
