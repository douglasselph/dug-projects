package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object RootAndScootBaseline : ConfiguredCardScorer(
    cardNames = setOf("Root_07_01"),
    effect = GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE,
    cultivationPlayBase = 40,
    battlePlayBase = 55
)
