package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object SnipHappensBaseline : ConfiguredCardScorer(
    cardNames = setOf("Vine_07_02"),
    effect = GameEffect.FLIP_OWN_PLANT_OR_FLIP_OPPONENT_ROOT_OR_VINE_IN_BATTLE,
    cultivationPlayBase = 55,
    battlePlayBase = 55
)
