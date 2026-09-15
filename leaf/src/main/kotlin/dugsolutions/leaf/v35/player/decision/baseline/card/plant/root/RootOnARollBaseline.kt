package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object RootOnARollBaseline : ConfiguredCardScorer(
    cardNames = setOf("Root_05_03"),
    effect = GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS,
    cultivationPlayBase = 45,
    battlePlayBase = 45
)
