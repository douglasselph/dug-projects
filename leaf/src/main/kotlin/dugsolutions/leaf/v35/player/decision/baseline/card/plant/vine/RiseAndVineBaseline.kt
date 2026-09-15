package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object RiseAndVineBaseline : ConfiguredCardScorer(
    cardNames = setOf("Vine_09_03"),
    effect = GameEffect.RAISE_ALL_DICE_PLUS_2,
    cultivationPlayBase = 72,
    battlePlayBase = 72
)
