package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object VineAndAgainBaseline : ConfiguredCardScorer(
    cardNames = setOf("Vine_07_03"),
    effect = GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT,
    cultivationPlayBase = 60,
    battlePlayBase = 60
)
