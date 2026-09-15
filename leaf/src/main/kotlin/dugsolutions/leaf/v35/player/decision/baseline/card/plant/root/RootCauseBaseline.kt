package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object RootCauseBaseline : ConfiguredCardScorer(
    cardNames = setOf("Root_09_02"),
    effect = GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE,
    cultivationPlayBase = 55,
    battlePlayBase = 55
)
