package dugsolutions.leaf.game.battle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.game.turn.select.SelectDieToAdjust
import dugsolutions.leaf.game.turn.select.SelectDieToMax
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.AppliedEffect

class HandleBattleEffects(
    private val chronicle: GameChronicle,
    private val selectDieToAdjust: SelectDieToAdjust,
    private val selectDieToMax: SelectDieToMax
) {

    operator fun invoke(player: Player) {
        for (effect in player.effectsList.copy()) {
            if (handleBattleEffect(player, effect)) {
                player.effectsList.remove(effect)
            }
        }
    }

    private fun handleBattleEffect(player: Player, effect: AppliedEffect) : Boolean {

        return when (effect) {
            is AppliedEffect.AdjustDieRoll -> {
                selectDieToAdjust(player.diceInHand, effect.adjustment)?.let { selectedDie ->
                    if (player.diceInHand.adjust(selectedDie, effect.adjustment)) {
                        chronicle(
                            GameChronicle.Moment.ADJUST_DIE(player, effect.adjustment)
                        )
                    }
                }
                true
            }

            is AppliedEffect.AdjustDieToMax -> {
                // Choose die with with the greatest difference against MAX.
                selectDieToMax(player.diceInHand)?.let { die ->
                    val amount = die.adjustToMax()
                    chronicle(GameChronicle.Moment.ADJUST_DIE(player, amount))
                }
                true
            }

            is AppliedEffect.AddToTotal -> {
                player.pipModifier += effect.amount
                chronicle(GameChronicle.Moment.ADD_TO_TOTAL(player, effect.amount))
                true
            }

            else -> { false }
        }
    }
}
