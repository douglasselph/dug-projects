package dugsolutions.leaf.v35.chronicle.domain

import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly

/** Immutable end-of-Round player state used only for Chronicle diagnostics. */
data class PlayerRoundSummarySnapshot(
    val playerId: PlayerId,
    val graftedPlantCount: Int,
    val supplyDice: List<DieSides>,
    val discardDice: List<DieSides>,
    val beeCount: Int,
    val wormCount: Int,
    val waterCount: Int,
    val mulchDice: List<DieSides?>,
    val wispCount: Int,
    val butterflies: List<Butterfly>
)
