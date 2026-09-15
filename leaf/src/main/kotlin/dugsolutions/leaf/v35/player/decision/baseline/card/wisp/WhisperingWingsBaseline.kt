package dugsolutions.leaf.v35.player.decision.baseline.card.wisp

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object WhisperingWingsBaseline : ConfiguredCardScorer(
    cardNames = setOf("Wisp_Gain_Critters"),
    effect = GameEffect.GAIN_ANY_TWO_CRITTERS,
    cultivationPlayBase = 60,
    battlePlayBase = 60
)
