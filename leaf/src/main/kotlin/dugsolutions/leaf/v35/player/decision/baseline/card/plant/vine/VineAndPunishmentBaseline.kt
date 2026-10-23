package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object VineAndPunishmentBaseline : ConfiguredCardScorer(
    cardNames = setOf("Vine_07_03"),
    effect = GameEffect.REROLL_DIE_ON_3_DRAW_ONE_CULTIVATION_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3,
    cultivationPlayBase = 35,
    battlePlayBase = 75
)
