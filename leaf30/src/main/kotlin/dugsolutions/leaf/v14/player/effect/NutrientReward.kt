package dugsolutions.leaf.v14.player.effect

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.random.di.DieFactory
import dugsolutions.leaf.v14.random.die.DieSides

class NutrientReward(
    private val dieFactory: DieFactory,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player) {
        val nutrients = player.nutrients
        val awardAmount = when {
            nutrients >= 10 -> 10
            nutrients >= 6 -> 6
            nutrients >= 5 -> 5
            nutrients >= 4 -> 4
            nutrients >= 3 -> 3
            nutrients >= 2 -> 2
            else -> return
        }
        val sides = awardAmount * 2
        val dieSides = DieSides.from(sides)
        player.addDieToDiscard(dieFactory(dieSides))
        player.nutrients -= awardAmount
        chronicle(Moment.NUTRIENT_REWARD(player, nutrients, dieSides))
    }
}
