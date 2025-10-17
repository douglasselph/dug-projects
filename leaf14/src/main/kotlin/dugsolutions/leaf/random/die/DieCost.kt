package dugsolutions.leaf.random.die

class DieCost {

    operator fun invoke(sides: Int): Int {
        return sides
    }

    operator fun invoke(die: Die): Int {
        return die.sides
    }

}
