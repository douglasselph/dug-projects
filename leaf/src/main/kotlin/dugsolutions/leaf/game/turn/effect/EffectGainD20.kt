package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.random.die.DieSides

class EffectGainD20(
    private val grove: Grove,
    private val dieFactory: DieFactory,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player) {
        if (grove.getDiceQuantity(20) > 0) {
            player.addDieToBed(dieFactory(DieSides.D20))
            chronicle(Moment.GAIN_D20(player))
        }
    }
}
