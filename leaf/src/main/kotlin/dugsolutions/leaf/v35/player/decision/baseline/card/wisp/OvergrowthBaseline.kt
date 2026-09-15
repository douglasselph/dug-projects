package dugsolutions.leaf.v35.player.decision.baseline.card.wisp

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object OvergrowthBaseline : ConfiguredCardScorer(
    cardNames = setOf("Wisp_Upgrade_Die"),
    effect = GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW,
    cultivationPlayBase = 85,
    battlePlayBase = 85
)
