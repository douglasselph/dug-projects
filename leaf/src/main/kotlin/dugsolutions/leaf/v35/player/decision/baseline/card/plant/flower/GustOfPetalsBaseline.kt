package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object GustOfPetalsBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_14_04"),
    effect = GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW,
    cultivationPlayBase = 48,
    battlePlayBase = 72
)
