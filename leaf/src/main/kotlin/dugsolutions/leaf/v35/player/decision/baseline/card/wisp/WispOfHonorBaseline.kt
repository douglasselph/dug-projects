package dugsolutions.leaf.v35.player.decision.baseline.card.wisp

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object WispOfHonorBaseline : ConfiguredCardScorer(
    cardNames = setOf("Wisp_Award_VP"),
    effect = GameEffect.CHOOSE_PLAYER_WITH_WISPS_TRASH_ONE,
    cultivationPlayBase = 22,
    battlePlayBase = 22
)
