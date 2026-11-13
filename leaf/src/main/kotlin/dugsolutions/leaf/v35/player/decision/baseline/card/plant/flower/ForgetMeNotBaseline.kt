package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object ForgetMeNotBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_17_02"),
    effect = GameEffect.USE_DIE_FROM_DISCARD_NOW,
    cultivationPlayBase = 72,
    battlePlayBase = 82
)
