package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object VineYieldBaseline : ConfiguredCardScorer(
    cardNames = setOf("Vine_07_04"),
    effect = GameEffect.RAISE_ONE_DIE_PLUS_1_PER_GRAFTED_FLOWER,
    cultivationPlayBase = 30,
    battlePlayBase = 30
)
