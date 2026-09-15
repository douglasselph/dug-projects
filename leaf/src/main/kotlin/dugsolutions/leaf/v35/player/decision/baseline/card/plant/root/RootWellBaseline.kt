package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object RootWellBaseline : ConfiguredCardScorer(
    cardNames = setOf("Root_05_04"),
    effect = GameEffect.GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE,
    cultivationPlayBase = 40,
    battlePlayBase = 65
)
