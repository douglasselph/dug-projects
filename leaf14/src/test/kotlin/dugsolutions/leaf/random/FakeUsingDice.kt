package dugsolutions.leaf.random

import dugsolutions.leaf.common.domain.acquire.UsingDice
import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.DieValues
import dugsolutions.leaf.random.die.SampleDie

object FakeUsingDice {

    val sampleDie = SampleDie()

    val d6 = UsingDice(
        DieValues(Dice(listOf(sampleDie.d6)).copy),
        addToTotal = 0
    )
    val d6Plus5 = UsingDice(
        DieValues(Dice(listOf(sampleDie.d6)).copy),
        addToTotal = 5
    )
    val d8 = UsingDice(
        DieValues(Dice(listOf(sampleDie.d8)).copy),
        addToTotal = 0
    )
    val d10 = UsingDice(
        DieValues(Dice(listOf(sampleDie.d10)).copy),
        addToTotal = 0
    )
    val d12 = UsingDice(
        DieValues(Dice(listOf(sampleDie.d12)).copy),
        addToTotal = 1
    )
    val d4D6 = UsingDice(
        DieValues(Dice(listOf(sampleDie.d4, sampleDie.d6)).copy),
        addToTotal = 0
    )
    val d4D6D8 = UsingDice(
        DieValues(Dice(listOf(sampleDie.d4, sampleDie.d6, sampleDie.d8)).copy),
        addToTotal = 0
    )

}
