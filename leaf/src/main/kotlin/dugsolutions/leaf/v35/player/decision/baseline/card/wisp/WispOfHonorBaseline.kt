package dugsolutions.leaf.v35.player.decision.baseline.card.wisp

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object WispOfHonorBaseline : ConfiguredCardScorer(
    cardNames = setOf("Wisp_Award_VP"),
    effect = GameEffect.GAIN_ONE_VP,
    cultivationPlayBase = 45,
    battlePlayBase = 45
)
