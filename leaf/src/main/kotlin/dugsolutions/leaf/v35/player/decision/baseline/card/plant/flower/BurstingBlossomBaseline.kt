package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object BurstingBlossomBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_17_01"),
    effect = GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE,
    cultivationPlayBase = 82,
    battlePlayBase = 82
)
