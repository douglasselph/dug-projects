package dugsolutions.leaf.game.effects

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.common.domain.GameEffect
import dugsolutions.leaf.game.effects.choices.EffectDieAdjust
import dugsolutions.leaf.game.effects.choices.EffectDieReroll
import dugsolutions.leaf.game.effects.choices.EffectDieRerollAny
import dugsolutions.leaf.game.effects.choices.EffectDieToMax
import dugsolutions.leaf.game.effects.choices.EffectDraw
import dugsolutions.leaf.game.effects.choices.EffectDrawCard
import dugsolutions.leaf.game.effects.choices.EffectDrawDie
import dugsolutions.leaf.game.turn.handle.HandleDieUpgrade
import dugsolutions.leaf.player.Player


class HandleGameEffect(
    private val effectDieAdjust: EffectDieAdjust,
    private val effectDieToMax: EffectDieToMax,
    private val effectDrawCard: EffectDrawCard,
    private val effectDrawDie: EffectDrawDie,
    private val effectDraw: EffectDraw,
    private val effectDieReroll: EffectDieReroll,
    private val effectDieRerollAny: EffectDieRerollAny,
    private val handleDieUpgrade: HandleDieUpgrade,
    private val chronicle: GameChronicle
) {

    operator fun invoke(
        player: Player,
        target: Player?,
        effect: GameEffect,
        value: Int
    ) {
        when (effect) {

            GameEffect.ADD_TO_DIE -> effectDieAdjust(player, value)

            GameEffect.ADJUST_BY -> {
                effectDieAdjust(player, value, target)
            }

            GameEffect.ADJUST_TO_MAX -> {
                repeat(value) { effectDieToMax(player) }
            }

            GameEffect.ADJUST_TO_MIN_OR_MAX -> {
                repeat(value) {
                    // TODO: Target player to MIN
                    effectDieToMax(player)
                }
            }

            GameEffect.DEFLECT -> {
                player.deflectDamage += value
                chronicle(Moment.DEFLECT_DAMAGE(player, value))
            }

            GameEffect.DRAW_CARD -> {
                repeat(value) { effectDrawCard(player) }
            }

            GameEffect.DRAW_CARD_DISCARD -> {
                repeat(value) { effectDrawCard(player, fromDiscard = true) }
            }

            GameEffect.DRAW_DIE -> {
                repeat(value) { effectDrawDie(player, EffectDrawDie.DrawDieParams()) }
            }

            GameEffect.DRAW_DIE_ANY -> {
                repeat(value) { effectDrawDie(player, EffectDrawDie.DrawDieParams(drawHighest = true)) }
            }

            GameEffect.DRAW_DIE_DISCARD -> {
                repeat(value) { effectDrawDie(player, EffectDrawDie.DrawDieParams(fromDiscard = true)) }
            }

            GameEffect.DRAW_ANY -> {
                repeat(value) { effectDraw(player) }
            }

            GameEffect.REROLL_ACCEPT_2ND -> {
                repeat(value) { effectDieReroll(player, takeBetter = false) }
            }

            GameEffect.REROLL_ANY -> {
                target?.let {
                    repeat(value) { effectDieRerollAny(player, target) }
                }
            }

            GameEffect.REROLL_TAKE_BETTER -> {
                repeat(value) { effectDieReroll(player, takeBetter = true) }
            }


            GameEffect.UPGRADE -> {
                repeat(value) { handleDieUpgrade(player, discardAfterUse = true) }
            }

            GameEffect.UPGRADE_RETAIN -> {
                repeat(value) { handleDieUpgrade(player, discardAfterUse = false) }
            }

            GameEffect.NONE -> TODO()
            GameEffect.DISCARD -> TODO()
            GameEffect.DISCARD_CARD -> TODO()
            GameEffect.DISCARD_DIE -> TODO()
            GameEffect.GRAFT_DIE -> TODO()
            GameEffect.RETAIN_CARD -> TODO()
            GameEffect.RETAIN_DIE -> TODO()
            GameEffect.RETAIN_DIE_REROLL -> TODO()
            GameEffect.REUSE_CARD -> TODO()
            GameEffect.REUSE_DIE -> TODO()
            GameEffect.REUSE_DIE_REROLL -> TODO()
            GameEffect.REUSE_ANY -> TODO()
            GameEffect.USE_OPPONENT_CARD -> TODO()
            GameEffect.USE_OPPONENT_DIE -> TODO()
        }
    }

}
