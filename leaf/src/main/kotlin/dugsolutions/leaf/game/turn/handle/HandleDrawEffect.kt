package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.AppliedEffect
import dugsolutions.leaf.player.effect.CardEffectProcessor
import kotlin.contracts.Effect

class HandleDrawEffect(
    private val cardEffectProcessor: CardEffectProcessor,
    private val cardManager: CardManager,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player) {
        for (effect in player.effectsList.copy()) {
            if (handleEffect(effect, player)) {
                player.effectsList.remove(effect)
            }
        }
    }

    private fun handleEffect(effect: AppliedEffect, player: Player): Boolean {
        return when (effect) {
            is AppliedEffect.DrawCards -> {
                repeat(effect.count) {
                    val cardId = if (effect.fromCompost) {
                        player.drawCardFromCompost()
                    } else {
                        player.drawCard()
                    }
                    cardId?.let {
                        chronicle(GameChronicle.Moment.DRAW_CARD(player, cardId))
                        handleRecursiveDraw(cardId, player)
                    }
                }
                true
            }

            is AppliedEffect.DrawDice -> {
                repeat(effect.count) {
                    val die = if (effect.fromCompost) {
                        if (effect.drawHighest) {
                            player.drawBestDieFromCompost()
                        } else {
                            player.drawDieFromCompost()
                        }
                    } else {
                        if (effect.drawHighest) {
                            player.drawBestDie()
                        } else {
                            player.drawDie()
                        }
                    }
                    die?.let { chronicle(GameChronicle.Moment.DRAW_DIE(player, die)) }
                }
                true
            }

            is AppliedEffect.DrawThenDiscard -> {
                // TODO: Draw then discard
                true
            }

            else -> false
        }
    }

    private fun handleRecursiveDraw(cardId: CardID, player: Player) {
        cardManager.getCard(cardId)?.let { card ->
            val newEffects = cardEffectProcessor.processCardEffect(card, player)
            for (effect in newEffects) {
                if (!handleEffect(effect, player)) {
                    player.effectsList.add(effect)
                }
            }
        }
    }
} 
