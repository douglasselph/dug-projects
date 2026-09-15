package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object RootAwakeningBaseline : ConfiguredCardScorer(
    cardNames = setOf("Root_09_01"),
    effect = GameEffect.UPGRADE_DIE_AND_USE_NOW,
    cultivationPlayBase = 75,
    battlePlayBase = 75
)
