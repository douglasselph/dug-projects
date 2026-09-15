package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object QueensBlossomBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_17_04"),
    effect = GameEffect.DRAW_TWO_DICE,
    cultivationPlayBase = 82,
    battlePlayBase = 82
)
