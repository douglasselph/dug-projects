package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object ForgetMeNotBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_17_02"),
    effect = GameEffect.ROLL_DIE_FROM_DISCARD_INTO_HAND,
    cultivationPlayBase = 72,
    battlePlayBase = 72
)
