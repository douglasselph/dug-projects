package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object RootFourMoreBaseline : ConfiguredCardScorer(
    cardNames = setOf("Root_05_02"),
    effect = GameEffect.RAISE_DIE_PLUS_4,
    cultivationPlayBase = 58,
    battlePlayBase = 58
)
