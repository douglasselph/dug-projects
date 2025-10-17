package dugsolutions.leaf.game.acquire.domain

import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.DieValues
import dugsolutions.leaf.random.die.SampleDie

object FakeCombination {

    val sampleDie = SampleDie()

    val combinationD6 = Combination(
        DieValues(Dice(listOf(sampleDie.d6)).copy),
        addToTotal = 0
    )
    val combinationD6Plus5 = Combination(
        DieValues(Dice(listOf(sampleDie.d6)).copy),
        addToTotal = 5
    )
    val combinationD8 = Combination(
        DieValues(Dice(listOf(sampleDie.d8)).copy),
        addToTotal = 0
    )
    val combinationD10 = Combination(
        DieValues(Dice(listOf(sampleDie.d10)).copy),
        addToTotal = 0
    )
    val combinationD12 = Combination(
        DieValues(Dice(listOf(sampleDie.d12)).copy),
        addToTotal = 1
    )
    val combinationD4D6 = Combination(
        DieValues(Dice(listOf(sampleDie.d4, sampleDie.d6)).copy),
        addToTotal = 0
    )
    val combinationD4D6D8 = Combination(
        DieValues(Dice(listOf(sampleDie.d4, sampleDie.d6, sampleDie.d8)).copy),
        addToTotal = 0
    )


}
