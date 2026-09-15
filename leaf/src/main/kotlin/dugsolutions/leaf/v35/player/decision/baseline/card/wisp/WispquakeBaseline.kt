package dugsolutions.leaf.v35.player.decision.baseline.card.wisp

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer

object WispquakeBaseline : ConfiguredCardScorer(
    cardNames = setOf("Wisp_Quake"),
    effect = GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN,
    cultivationPlayBase = 50,
    battlePlayBase = 50
)
