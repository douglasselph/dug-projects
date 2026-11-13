package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object BloomBackflipBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_14_02"),
    effect = GameEffect.FLIP_OWN_DIE_AND_OPPONENT_DIE_IN_BATTLE,
    cultivationPlayBase = 35,
    battlePlayBase = 78
)
