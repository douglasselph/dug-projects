package dugsolutions.leaf.v35.player.decision.baseline.card.wisp

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object PollenTheftBaseline : ConfiguredCardScorer(
    cardNames = setOf("Wisp_Swap_Die"),
    effect = GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE,
    cultivationPlayBase = 20,
    battlePlayBase = 68
)
