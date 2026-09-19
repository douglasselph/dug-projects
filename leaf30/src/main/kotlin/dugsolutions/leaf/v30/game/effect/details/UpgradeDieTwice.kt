package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieSides
import dugsolutions.leaf.v30.random.die.di.DieFactory

class UpgradeDieTwice(
    private val grove: Grove,
    private val dieFactory: DieFactory = DieFactory(Randomizer.create())
) {
    operator fun invoke(die: Die): Die? {
        val current = DieSides.from(die.sides)
        val next = findSecondAvailableUpgrade(current) ?: return null

        grove.remove(next)
        if (current == DieSides.D4) {
            grove.add(DieSides.D4)
        }
        return dieFactory(next)
    }

    private fun findSecondAvailableUpgrade(current: DieSides): DieSides? {
        var found = 0
        upgradeOrder.dropWhile { it != current }
            .drop(1)
            .forEach { sides ->
                if (grove.has(sides)) {
                    found++
                    if (found == NUM_UPGRADES) return sides
                }
            }
        return null
    }

    private companion object {
        const val NUM_UPGRADES = 2
        val upgradeOrder = DieSides.entries
    }
}
