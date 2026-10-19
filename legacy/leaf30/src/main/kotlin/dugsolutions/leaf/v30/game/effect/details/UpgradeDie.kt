package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieSides
import dugsolutions.leaf.v30.random.die.di.DieFactory

class UpgradeDie(
    private val grove: Grove,
    private val dieFactory: DieFactory = DieFactory(Randomizer.create())
) {
    operator fun invoke(die: Die): Die? {
        val current = DieSides.from(die.sides)
        val next = upgradeOrder.nextAfter(current) ?: return null
        if (!grove.has(next)) return null

        grove.remove(next)
        if (current == DieSides.D4) {
            grove.add(DieSides.D4)
        }
        return dieFactory(next)
    }

    private companion object {
        val upgradeOrder = DieSides.entries

        fun List<DieSides>.nextAfter(sides: DieSides): DieSides? {
            val index = indexOf(sides)
            if (index < 0) return null
            return getOrNull(index + 1)
        }
    }
}
