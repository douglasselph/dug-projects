package dugsolutions.leaf.v35.player.decision.baseline.card.wisp

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object BerryPatientBaseline : ConfiguredCardScorer(
    cardNames = setOf("Wisp_Award_VP2"),
    effect = GameEffect.EACH_PLAYER_WITH_WISPS_TRASH_ONE,
    cultivationPlayBase = 20,
    battlePlayBase = 20
)
