package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object RootDownPaymentBaseline : ConfiguredCardScorer(
    cardNames = setOf("Root_07_03"),
    effect = GameEffect.GAIN_VP_PER_ONE_SHOWING,
    cultivationPlayBase = 45,
    battlePlayBase = 45
)
