package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object PetalToDie4Baseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_14_04"),
    effect = GameEffect.GAIN_D4_USE_NOW_THEN_CYCLE_D4S,
    cultivationPlayBase = 65,
    battlePlayBase = 65
)
