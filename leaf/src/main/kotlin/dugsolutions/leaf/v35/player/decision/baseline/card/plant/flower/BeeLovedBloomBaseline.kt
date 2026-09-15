package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object BeeLovedBloomBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_14_01"),
    effect = GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND,
    cultivationPlayBase = 68,
    battlePlayBase = 68
)
