package dugsolutions.leaf.player.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.random.die.DieSides

// TODO: Unit test
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
        player.addDieToBed(dieFactory(dieSides))
        player.nutrients -= awardAmount
        chronicle(Moment.NUTRIENT_REWARD(player, nutrients, dieSides))
    }
}
