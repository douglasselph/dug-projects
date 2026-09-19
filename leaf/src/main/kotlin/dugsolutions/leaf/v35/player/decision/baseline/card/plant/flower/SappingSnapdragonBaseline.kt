package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object SappingSnapdragonBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_11_03"),
    effect = GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW,
    cultivationPlayBase = 55,
    battlePlayBase = 82
)
