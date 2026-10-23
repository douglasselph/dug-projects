package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object PetalToDie4Baseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_14_04"),
    effect = GameEffect.GAIN_OR_STEAL_D4_THEN_DISCARD_D4S_AND_DRAW,
    cultivationPlayBase = 65,
    battlePlayBase = 65
)
