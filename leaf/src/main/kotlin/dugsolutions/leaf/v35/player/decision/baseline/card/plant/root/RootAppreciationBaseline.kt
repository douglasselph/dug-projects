package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object RootAppreciationBaseline : ConfiguredCardScorer(
    cardNames = setOf("Root_07_02"),
    effect = GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND,
    cultivationPlayBase = 60,
    battlePlayBase = 60
)
