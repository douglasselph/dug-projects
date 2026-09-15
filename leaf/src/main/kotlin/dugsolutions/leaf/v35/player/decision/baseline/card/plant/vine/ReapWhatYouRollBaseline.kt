package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object ReapWhatYouRollBaseline : ConfiguredCardScorer(
    cardNames = setOf("Vine_11_01"),
    effect = GameEffect.DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE,
    cultivationPlayBase = 70,
    battlePlayBase = 70
)
