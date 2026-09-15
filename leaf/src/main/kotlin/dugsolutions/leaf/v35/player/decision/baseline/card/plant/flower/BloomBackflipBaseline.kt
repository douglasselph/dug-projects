package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object BloomBackflipBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_14_03"),
    effect = GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW,
    cultivationPlayBase = 35,
    battlePlayBase = 78
)
