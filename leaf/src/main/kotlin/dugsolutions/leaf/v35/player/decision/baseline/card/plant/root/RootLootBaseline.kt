package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object RootLootBaseline : ConfiguredCardScorer(
    cardNames = setOf("Root_09_04"),
    effect = GameEffect.MULCH_DIE_FROM_DISCARD,
    cultivationPlayBase = 65,
    battlePlayBase = 65
)
