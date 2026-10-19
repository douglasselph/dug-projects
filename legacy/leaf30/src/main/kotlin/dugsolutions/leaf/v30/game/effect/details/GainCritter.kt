package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.grove.Grove

class GainCritter(
    private val grove: Grove
) {
    operator fun invoke(critter: Critter): Critter? {
        val normal = critter.normal
        if (!grove.has(normal)) return null
        grove.remove(normal)
        return normal
    }
}
