package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object LowAndBeholdBaseline : ConfiguredCardScorer(
    cardNames = setOf("Vine_09_01"),
    effect = GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX,
    cultivationPlayBase = 68,
    battlePlayBase = 68
)
