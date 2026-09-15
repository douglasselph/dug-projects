package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object VinesTheLimitBaseline : ConfiguredCardScorer(
    cardNames = setOf("Vine_11_04"),
    effect = GameEffect.SET_DIE_UP_TO_D12_TO_MAX,
    cultivationPlayBase = 70,
    battlePlayBase = 70
)
