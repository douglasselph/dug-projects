package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object SnipHappensBaseline : ConfiguredCardScorer(
    cardNames = setOf("Vine_07_02"),
    effect = GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE,
    cultivationPlayBase = 55,
    battlePlayBase = 55
)
