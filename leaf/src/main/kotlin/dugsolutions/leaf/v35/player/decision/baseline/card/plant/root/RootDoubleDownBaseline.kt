package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object RootDoubleDownBaseline : ConfiguredCardScorer(
    cardNames = setOf("Root_05_01"),
    effect = GameEffect.DOUBLE_ONE_DIE,
    cultivationPlayBase = 55,
    battlePlayBase = 55
)
