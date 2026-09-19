package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object AlluringNectarBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_11_01"),
    effect = GameEffect.GAIN_OR_STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES,
    cultivationPlayBase = 60,
    battlePlayBase = 60
)
