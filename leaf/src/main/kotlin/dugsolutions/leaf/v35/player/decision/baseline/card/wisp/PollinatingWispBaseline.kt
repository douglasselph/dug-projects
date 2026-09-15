package dugsolutions.leaf.v35.player.decision.baseline.card.wisp

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object PollinatingWispBaseline : ConfiguredCardScorer(
    cardNames = setOf("Wisp_Gain_Green", "Wisp_Gain_Purple", "Wisp_Gain_Red", "Wisp_Gain_Yellow"),
    effect = GameEffect.GAIN_OR_REFRESH_GREEN_BUTTERFLY,
    cultivationPlayBase = 58,
    battlePlayBase = 58
)
