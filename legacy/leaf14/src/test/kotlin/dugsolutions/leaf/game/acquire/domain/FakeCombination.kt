package dugsolutions.leaf.game.acquire.domain

import dugsolutions.leaf.common.domain.acquire.UsingDice
import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.DieValues
import dugsolutions.leaf.random.die.SampleDie

object FakeUsingDice {

    val sampleDie = SampleDie()

    val combinationD6 = UsingDice(
        DieValues(Dice(listOf(sampleDie.d6)).copy),
        addToTotal = 0
    )
    val combinationD6Plus5 = UsingDice(
        DieValues(Dice(listOf(sampleDie.d6)).copy),
        addToTotal = 5
    )
    val combinationD8 = UsingDice(
        DieValues(Dice(listOf(sampleDie.d8)).copy),
        addToTotal = 0
    )
    val combinationD10 = UsingDice(
        DieValues(Dice(listOf(sampleDie.d10)).copy),
        addToTotal = 0
    )
    val combinationD12 = UsingDice(
        DieValues(Dice(listOf(sampleDie.d12)).copy),
        addToTotal = 1
    )
    val combinationD4D6 = UsingDice(
        DieValues(Dice(listOf(sampleDie.d4, sampleDie.d6)).copy),
        addToTotal = 0
    )
    val combinationD4D6D8 = UsingDice(
        DieValues(Dice(listOf(sampleDie.d4, sampleDie.d6, sampleDie.d8)).copy),
        addToTotal = 0
    )


}
