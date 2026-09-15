package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object AlluringNectarBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_11_02"),
    effect = GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES,
    cultivationPlayBase = 60,
    battlePlayBase = 60
)
