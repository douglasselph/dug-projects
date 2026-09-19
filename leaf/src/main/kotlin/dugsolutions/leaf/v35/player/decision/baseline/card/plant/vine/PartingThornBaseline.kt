package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object PartingThornBaseline : ConfiguredCardScorer(
    cardNames = setOf("Vine_09_02"),
    effect = GameEffect.FLIP_OWN_PLANT_OR_WOUND_CHOSEN_OPPONENT_CHOOSE_CARD_IN_BATTLE,
    cultivationPlayBase = 45,
    battlePlayBase = 80
)
