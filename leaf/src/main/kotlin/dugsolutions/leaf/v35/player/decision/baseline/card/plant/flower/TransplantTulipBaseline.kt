package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object TransplantTulipBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_11_04"),
    effect = GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE,
    cultivationPlayBase = 50,
    battlePlayBase = 65
)
