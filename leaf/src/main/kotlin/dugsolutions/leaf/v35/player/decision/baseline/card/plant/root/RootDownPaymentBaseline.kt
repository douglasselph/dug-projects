package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object RootDownPaymentBaseline : ConfiguredCardScorer(
    cardNames = setOf("Root_07_03"),
    effect = GameEffect.SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE,
    cultivationPlayBase = 45,
    battlePlayBase = 45
)
