package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object BloomBackboneBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_14_02"),
    effect = GameEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER,
    cultivationPlayBase = 70,
    battlePlayBase = 70
)
