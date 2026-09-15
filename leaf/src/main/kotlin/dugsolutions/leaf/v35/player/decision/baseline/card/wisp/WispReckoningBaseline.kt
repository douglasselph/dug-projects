package dugsolutions.leaf.v35.player.decision.baseline.card.wisp

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object WispReckoningBaseline : ConfiguredCardScorer(
    cardNames = setOf("Wisp_Reckoning"),
    effect = GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS,
    cultivationPlayBase = 55,
    battlePlayBase = 55
)
