package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object PetalToDie4Baseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_14_04"),
    effect = GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4,
    cultivationPlayBase = 65,
    battlePlayBase = 65
)
