package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object SaplinkTrellisBaseline : ConfiguredCardScorer(
    cardNames = setOf("Vine_11_02"),
    effect = GameEffect.RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE,
    cultivationPlayBase = 65,
    battlePlayBase = 65
)
