package dugsolutions.leaf.v35.player.decision.baseline.card.wisp

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object PocketedSparkBaseline : ConfiguredCardScorer(
    cardNames = setOf("Wisp_Mulch_Die"),
    effect = GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD,
    cultivationPlayBase = 72,
    battlePlayBase = 72
)
