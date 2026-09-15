package dugsolutions.leaf.v35.player.decision.baseline.card.wisp

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object WispsLastWordBaseline : ConfiguredCardScorer(
    cardNames = setOf("Wisps_Last_Word"),
    effect = GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW,
    cultivationPlayBase = 20,
    battlePlayBase = 75
)
