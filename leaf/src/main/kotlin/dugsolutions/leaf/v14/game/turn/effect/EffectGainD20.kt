package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.grove.Grove
import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.random.di.DieFactory
import dugsolutions.leaf.v14.random.die.DieSides

class EffectGainD20(
    private val grove: Grove,
    private val dieFactory: DieFactory,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player) {
        if (grove.getDiceQuantity(20) > 0) {
            player.addDieToDiscard(dieFactory(DieSides.D20))
            chronicle(Moment.GAIN_D20(player))
        }
    }
}
