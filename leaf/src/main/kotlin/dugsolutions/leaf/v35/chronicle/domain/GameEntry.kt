package dugsolutions.leaf.v35.chronicle.domain

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter

/**
 * Immutable recorded history for one game/simulation.
 *
 * Entries contain scalar values, enums/value objects, and defensive immutable
 * list snapshots only. They never retain mutable gameplay objects.
 */
sealed interface GameEntry {
    val sequence: Long

    data class Marker(override val sequence: Long, val message: String) : GameEntry

    data class RoundRevealed(
        override val sequence: Long,
        val roundNumber: Int,
        val cardName: String,
        val cardType: RoundCardType,
        val firstEffect: GameEffect,
        val secondEffect: GameEffect
    ) : GameEntry

    data class RoundCompleted(
        override val sequence: Long,
        val roundNumber: Int,
        val cardName: String,
        val cardType: RoundCardType
    ) : GameEntry

    data class DieRolled(
        override val sequence: Long,
        val playerId: PlayerId,
        val sides: Int,
        val value: Int,
        val rewardPolicy: ChronicleRollRewardPolicy,
        val reason: RollReason
    ) : GameEntry

    data class RollReward(
        override val sequence: Long,
        val playerId: PlayerId,
        val kind: RollRewardKind,
        val critter: Critter?,
        val wispName: String?
    ) : GameEntry

    data class OpeningDrawCompleted(
        override val sequence: Long,
        val phase: ChroniclePhase,
        val playerId: PlayerId,
        val count: Int
    ) : GameEntry

    data class MainAction(
        override val sequence: Long,
        val playerId: PlayerId,
        val phase: ChroniclePhase,
        val action: MainActionKind,
        val actionNumber: Int?,
        val battleStage: BattleMainStage?
    ) : GameEntry

    data class SupportAction(
        override val sequence: Long,
        val playerId: PlayerId,
        val phase: ChroniclePhase,
        val action: SupportActionKind,
        val row: StrikeRow?
    ) : GameEntry

    data class EffectResolved(
        override val sequence: Long,
        val playerId: PlayerId,
        val effect: GameEffect,
        val sourceKind: EffectSourceKind,
        val sourceName: String,
        val phase: ChroniclePhase
    ) : GameEntry

    data class BuyOrder(
        override val sequence: Long,
        val order: List<PlayerId>
    ) : GameEntry

    data class Purchase(
        override val sequence: Long,
        val playerId: PlayerId,
        val kind: PurchaseKind,
        val itemName: String,
        val cost: Int,
        val paymentTotal: Int
    ) : GameEntry {
        val overpayment: Int get() = paymentTotal - cost
    }

    data class Graft(
        override val sequence: Long,
        val playerId: PlayerId,
        val plantName: String
    ) : GameEntry

    data class BattleOrder(
        override val sequence: Long,
        val order: List<PlayerId>,
        val initialDiceCount: Int
    ) : GameEntry

    data class StrikeResolved(
        override val sequence: Long,
        val row: StrikeRow,
        val totals: List<StrikeTotalSnapshot>,
        val winnerIds: List<PlayerId>,
        val woundedPlayerIds: List<PlayerId>,
        val vpPerWinner: Int
    ) : GameEntry

    data class Wound(
        override val sequence: Long,
        val playerId: PlayerId,
        val kind: WoundKind,
        val plantName: String
    ) : GameEntry

    data class Doom(
        override val sequence: Long,
        val dice: List<DoomDieSnapshot>
    ) : GameEntry {
        val count: Int get() = dice.size
        val valuesTrashed: List<Int> get() = dice.map { it.value }.distinct()
    }

    data class Refresh(
        override val sequence: Long,
        val playerId: PlayerId
    ) : GameEntry

    data class Cleanup(
        override val sequence: Long,
        val playerId: PlayerId,
        val phase: ChroniclePhase,
        val discardedDice: Int,
        val returnedCritters: Int,
        val refreshed: Boolean
    ) : GameEntry

    data class Upgrade(
        override val sequence: Long,
        val playerId: PlayerId,
        val from: DieSides,
        val to: DieSides,
        val destination: UpgradeDestination
    ) : GameEntry

    data class TrashDie(
        override val sequence: Long,
        val playerId: PlayerId,
        val sides: DieSides,
        val destination: TrashDestination
    ) : GameEntry

    data class FinalScore(
        override val sequence: Long,
        val playerId: PlayerId,
        val existingVp: Int,
        val plantVp: Int,
        val unplayedWispVp: Int,
        val totalVp: Int,
        val graftedPlantCount: Int
    ) : GameEntry

    data class FinalWinners(
        override val sequence: Long,
        val winnerIds: List<PlayerId>
    ) : GameEntry

    data class GameCompleted(
        override val sequence: Long,
        val roundsCompleted: Int
    ) : GameEntry
}
