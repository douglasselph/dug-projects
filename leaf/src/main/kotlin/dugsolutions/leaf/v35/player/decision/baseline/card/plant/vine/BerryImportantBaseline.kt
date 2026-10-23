package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object BerryImportantBaseline : ConfiguredCardScorer(
    cardNames = setOf("Vine_11_02"),
    effect = GameEffect.RAISE_ALL_D8S_PLUS_1,
    cultivationPlayBase = 30,
    battlePlayBase = 30
)
