package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.effect.EffectCardToRetain
import dugsolutions.leaf.game.turn.effect.EffectDieAdjust
import dugsolutions.leaf.game.turn.effect.EffectDieReroll
import dugsolutions.leaf.game.turn.effect.EffectDieToMax
import dugsolutions.leaf.game.turn.effect.EffectDieToRetain
import dugsolutions.leaf.game.turn.effect.EffectDiscard
import dugsolutions.leaf.game.turn.effect.EffectDraw
import dugsolutions.leaf.game.turn.effect.EffectDrawCard
import dugsolutions.leaf.game.turn.effect.EffectDrawDie
import dugsolutions.leaf.game.turn.effect.EffectGainD20
import dugsolutions.leaf.game.turn.effect.EffectReplayVine
import dugsolutions.leaf.game.turn.effect.EffectReuse
import dugsolutions.leaf.game.turn.effect.EffectReuseCard
import dugsolutions.leaf.game.turn.effect.EffectReuseDie
import dugsolutions.leaf.game.turn.effect.EffectUseOpponentCard
import dugsolutions.leaf.game.turn.effect.EffectUseOpponentDie
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.AppliedEffect
import dugsolutions.leaf.random.die.DieSides

class HandleCardEffect(
    private val effectCardToRetain: EffectCardToRetain,
    private val effectDieAdjust: EffectDieAdjust,
    private val effectDieToMax: EffectDieToMax,
    private val effectDiscard: EffectDiscard,
    private val effectDrawCard: EffectDrawCard,
    private val effectDrawDie: EffectDrawDie,
    private val effectDraw: EffectDraw,
    private val effectDieReroll: EffectDieReroll,
    private val effectDieToRetain: EffectDieToRetain,
    private val effectGainD20: EffectGainD20,
    private val effectReuseCard: EffectReuseCard,
    private val effectReuseDie: EffectReuseDie,
    private val effectReuse: EffectReuse,
    private val effectReplayVine: EffectReplayVine,
    private val handleDieUpgrade: HandleDieUpgrade,
    private val effectUseOpponentCard: EffectUseOpponentCard,
    private val effectUseOpponentDie: EffectUseOpponentDie,
    private val chronicle: GameChronicle
) {

    operator fun invoke(
        player: Player,
        target: Player,
        effect: CardEffect,
        value: Int
    ) {
        when (effect) {

            CardEffect.ADD_TO_DIE -> effectDieAdjust(player, value)

            CardEffect.ADD_TO_TOTAL -> {
                player.pipModifier += value
                chronicle(Moment.ADD_TO_TOTAL(player, value))
            }

            CardEffect.ADJUST_BY -> {
                effectDieAdjust(player, value, target)
            }

            CardEffect.ADJUST_TO_MAX -> {
                repeat(value) { effectDieToMax(player) }
            }

            CardEffect.ADJUST_TO_MIN_OR_MAX -> {
                repeat(value) {
                    // TODO: Target player to MIN
                    effectDieToMax(player)
                }
            }

            CardEffect.ADORN -> {
                throw Exception("ADORN effect should have already been handled")
            }

            CardEffect.DEFLECT -> {
                player.deflectDamage += value
                chronicle(Moment.DEFLECT_DAMAGE(player, value))
            }

            CardEffect.DISCARD -> {
                repeat(value) { effectDiscard(EffectDiscard.DiscardWhich.BOTH, target) }
            }

            CardEffect.DISCARD_CARD -> {
                repeat(value) { effectDiscard(EffectDiscard.DiscardWhich.CARDS, target) }
            }

            CardEffect.DISCARD_DIE -> {
                repeat(value) { effectDiscard(EffectDiscard.DiscardWhich.DICE, target) }
            }

            CardEffect.DRAW_CARD -> {
                repeat(value) { effectDrawCard(player) }
            }

            CardEffect.DRAW_CARD_BED -> {
                repeat(value) { effectDrawCard(player, fromCompost = true) }
            }

            CardEffect.DRAW_DIE -> {
                repeat(value) { effectDrawDie(player, EffectDrawDie.DrawDieParams()) }
            }

            CardEffect.DRAW_DIE_ANY -> {
                repeat(value) { effectDrawDie(player, EffectDrawDie.DrawDieParams(drawHighest = true)) }
            }

            CardEffect.DRAW_DIE_BED -> {
                repeat(value) { effectDrawDie(player, EffectDrawDie.DrawDieParams(fromCompost = true)) }
            }

            CardEffect.DRAW -> {
                repeat(value) { effectDraw(player) }
            }

            CardEffect.FLOURISH_OVERRIDE -> {
                player.delayedEffectList.add(AppliedEffect.FlourishOverride)
            }

            CardEffect.GAIN_D20 -> {
                repeat(value) { effectGainD20(player) }
            }

            CardEffect.GAIN_FREE_ROOT -> {
                player.delayedEffectList.add(
                    AppliedEffect.MarketBenefit(
                        type = FlourishType.ROOT,
                        costReduction = 0,
                        isFree = true,
                    )
                )
            }

            CardEffect.GAIN_FREE_CANOPY -> {
                player.delayedEffectList.add(
                    AppliedEffect.MarketBenefit(
                        type = FlourishType.CANOPY,
                        costReduction = 0,
                        isFree = true,
                    )
                )
            }

            CardEffect.GAIN_FREE_VINE -> {
                player.delayedEffectList.add(
                    AppliedEffect.MarketBenefit(
                        type = FlourishType.VINE,
                        costReduction = 0,
                        isFree = true
                    )
                )
            }

            CardEffect.REDUCE_COST_ROOT -> {
                player.delayedEffectList.add(
                    AppliedEffect.MarketBenefit(
                        type = FlourishType.ROOT,
                        costReduction = value
                    )
                )
            }

            CardEffect.REDUCE_COST_CANOPY -> {
                player.delayedEffectList.add(
                    AppliedEffect.MarketBenefit(
                        type = FlourishType.CANOPY,
                        costReduction = value
                    )
                )
            }

            CardEffect.REDUCE_COST_VINE -> {
                player.delayedEffectList.add(
                    AppliedEffect.MarketBenefit(
                        type = FlourishType.VINE,
                        costReduction = value
                    )
                )
            }

            CardEffect.REROLL_ACCEPT_2ND -> {
                repeat(value) {
                    effectDieReroll(player, takeBetter = false)
                }
            }

            CardEffect.REROLL_ALL_MAX -> {
                TODO("Not implemented")
            }

            CardEffect.REROLL_TAKE_BETTER -> {
                repeat(value) { effectDieReroll(player, takeBetter = true) }
            }

            CardEffect.RETAIN_CARD -> {
                repeat(value) { effectCardToRetain(player) }
            }

            CardEffect.RETAIN_DIE -> {
                repeat(value) { effectDieToRetain(player) }
            }

            CardEffect.RETAIN_DIE_REROLL -> {
                repeat(value) { effectDieToRetain(player, withReroll = true) }
            }

            CardEffect.REUSE_CARD -> {
                repeat(value) { effectReuseCard(player) }
            }

            CardEffect.REUSE_DIE -> {
                repeat(value) { effectReuseDie(player, rerollOkay = false) }
            }

            CardEffect.REUSE_DIE_REROLL -> {
                repeat(value) { effectReuseDie(player, rerollOkay = true) }
            }

            CardEffect.REUSE_ANY -> {
                repeat(value) { effectReuse(player) }
            }

            CardEffect.REPLAY_VINE -> {
                repeat(value) { effectReplayVine(player) }
            }

            CardEffect.UPGRADE_ANY_RETAIN -> {
                repeat(value) { handleDieUpgrade(player) }
            }

            CardEffect.UPGRADE_ANY -> {
                repeat(value) { handleDieUpgrade(player, discardAfterUse = true) }
            }

            CardEffect.UPGRADE_D4 -> {
                repeat(value) { handleDieUpgrade(player, only = listOf(DieSides.D4)) }
            }

            CardEffect.UPGRADE_D6 -> {
                repeat(value) {
                    handleDieUpgrade(
                        player,
                        only = listOf(DieSides.D4, DieSides.D6),
                        discardAfterUse = true
                    )
                }
            }

            CardEffect.UPGRADE_D8 -> {
                repeat(value) {
                    handleDieUpgrade(
                        player,
                        only = listOf(DieSides.D4, DieSides.D6, DieSides.D8),
                        discardAfterUse = true
                    )
                }
            }

            CardEffect.UPGRADE_D10 -> repeat(value) {
                handleDieUpgrade(
                    player,
                    only = listOf(DieSides.D4, DieSides.D6, DieSides.D8, DieSides.D10),
                    discardAfterUse = true
                )
            }

            CardEffect.USE_OPPONENT_CARD -> {
                repeat(value) { effectUseOpponentCard(player, target) }
            }

            CardEffect.USE_OPPONENT_DIE -> {
                repeat(value) { effectUseOpponentDie(player, target) }
            }
        }
    }

}
