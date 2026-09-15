package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object VineAndPunishmentBaseline : ConfiguredCardScorer(
    cardNames = setOf("Vine_11_03"),
    effect = GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3,
    cultivationPlayBase = 35,
    battlePlayBase = 75
)
