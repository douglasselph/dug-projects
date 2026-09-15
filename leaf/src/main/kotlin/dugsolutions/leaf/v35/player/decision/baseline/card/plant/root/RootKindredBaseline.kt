package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object RootKindredBaseline : ConfiguredCardScorer(
    cardNames = setOf("Root_09_03"),
    effect = GameEffect.SET_DIE_TO_MATCH_ANOTHER,
    cultivationPlayBase = 55,
    battlePlayBase = 55
)
