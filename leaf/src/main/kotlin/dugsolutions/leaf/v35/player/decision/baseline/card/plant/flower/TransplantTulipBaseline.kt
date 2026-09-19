package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object TransplantTulipBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_11_04"),
    effect = GameEffect.DRAW_ONE_DIE_AND_SWAP_TWO_OWN_DICE_RAISE_ONE_PLUS_2_IN_BATTLE,
    cultivationPlayBase = 50,
    battlePlayBase = 65
)
