package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object RootRecallBaseline : ConfiguredCardScorer(
    cardNames = setOf("Root_09_04"),
    effect = GameEffect.DISCARD_UP_TO_2_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE,
    cultivationPlayBase = 55,
    battlePlayBase = 55
)
