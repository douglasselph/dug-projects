package dugsolutions.leaf.v35.chronicle.domain

import dugsolutions.leaf.v35.battle.domain.BattleGridRowSnapshot
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
    val hierarchyDepth: Int

    data class Marker(override val sequence: Long, val message: String, override val hierarchyDepth: Int = 0) : GameEntry

    data class RoundRevealed(
        override val sequence: Long,
        val roundNumber: Int,
        val cardName: String,
        val cardType: RoundCardType,
        val firstEffect: GameEffect,
        val secondEffect: GameEffect,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class RoundCompleted(
        override val sequence: Long,
        val roundNumber: Int,
        val cardName: String,
        val cardType: RoundCardType,
        val playerSummaries: List<PlayerRoundSummarySnapshot> = emptyList(),
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class DieRolled(
        override val sequence: Long,
        val playerId: PlayerId,
        val sides: Int,
        val value: Int,
        val rewardPolicy: ChronicleRollRewardPolicy,
        val reason: RollReason,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class RollReward(
        override val sequence: Long,
        val playerId: PlayerId,
        val kind: RollRewardKind,
        val critter: Critter?,
        val wispName: String?,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class OpeningDrawCompleted(
        override val sequence: Long,
        val phase: ChroniclePhase,
        val playerId: PlayerId,
        val count: Int,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class MainAction(
        override val sequence: Long,
        val playerId: PlayerId,
        val phase: ChroniclePhase,
        val action: MainActionKind,
        val actionNumber: Int?,
        val battleStage: BattleMainStage?,
        val decisionProbabilityPercent: Int? = null,
        override val hierarchyDepth: Int = 0
    ) : GameEntry {
        init {
            require(decisionProbabilityPercent == null || decisionProbabilityPercent in 0..100) {
                "Main Action decision probability must be 0..100: $decisionProbabilityPercent"
            }
        }
    }

    data class SupportAction(
        override val sequence: Long,
        val playerId: PlayerId,
        val phase: ChroniclePhase,
        val action: SupportActionKind,
        val row: StrikeRow?,
        val wispUsePercentage: Int? = null,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class EffectResolved(
        override val sequence: Long,
        val playerId: PlayerId,
        val effect: GameEffect,
        val sourceKind: EffectSourceKind,
        val sourceName: String,
        val phase: ChroniclePhase,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    /** Optional recorded reasoning for a strategy-selected choice. */
    data class DecisionReasoning(
        override val sequence: Long,
        val playerId: PlayerId,
        val choiceLabel: String,
        val baseScore: Int,
        val adjustments: List<DecisionScoreAdjustmentSnapshot>,
        val total: Int,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class BuyOrder(
        override val sequence: Long,
        val order: List<PlayerId>,
        val leaderDie: BuyOrderLeadDieSnapshot? = null,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class Purchase(
        override val sequence: Long,
        val playerId: PlayerId,
        val kind: PurchaseKind,
        val itemName: String,
        val cost: Int,
        val paymentTotal: Int,
        override val hierarchyDepth: Int = 0
    ) : GameEntry {
        val overpayment: Int get() = paymentTotal - cost
    }

    data class Graft(
        override val sequence: Long,
        val playerId: PlayerId,
        val plantName: String,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class BattleOrder(
        override val sequence: Long,
        val order: List<PlayerId>,
        val initialDiceCount: Int,
        val highestDice: List<BattleOrderHighDieSnapshot> = emptyList(),
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class BattleGridReport(
        override val sequence: Long,
        val kind: BattleGridReportKind,
        val passNumber: Int?,
        val rows: List<BattleGridRowSnapshot>,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class ButterflyState(
        override val sequence: Long,
        val playerId: PlayerId,
        val butterflies: List<ButterflyStateSnapshot>,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class StrikeResolved(
        override val sequence: Long,
        val row: StrikeRow,
        val totals: List<StrikeTotalSnapshot>,
        val rowSnapshot: BattleGridRowSnapshot? = null,
        val winnerIds: List<PlayerId>,
        val woundedPlayerIds: List<PlayerId>,
        val vpPerWinner: Int,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class Wound(
        override val sequence: Long,
        val playerId: PlayerId,
        val kind: WoundKind,
        val plantName: String,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class Doom(
        override val sequence: Long,
        val dice: List<DoomDieSnapshot>,
        override val hierarchyDepth: Int = 0
    ) : GameEntry {
        val count: Int get() = dice.size
        val valuesTrashed: List<Int> get() = dice.map { it.value }.distinct()
    }

    data class Refresh(
        override val sequence: Long,
        val playerId: PlayerId,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class Cleanup(
        override val sequence: Long,
        val playerId: PlayerId,
        val phase: ChroniclePhase,
        val discardedDice: Int,
        val returnedCritters: Int,
        val refreshed: Boolean,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class Upgrade(
        override val sequence: Long,
        val playerId: PlayerId,
        val from: DieSides,
        val to: DieSides,
        val destination: UpgradeDestination,
        val fromValue: Int? = null,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class MulchStored(
        override val sequence: Long,
        val playerId: PlayerId,
        val sides: DieSides,
        val value: Int,
        val fromDiscard: Boolean,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class DieValueChanged(
        override val sequence: Long,
        val playerId: PlayerId,
        val effect: GameEffect,
        val sides: DieSides,
        val before: Int,
        val after: Int,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class TrashDie(
        override val sequence: Long,
        val playerId: PlayerId,
        val sides: DieSides,
        val destination: TrashDestination,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class FinalScore(
        override val sequence: Long,
        val playerId: PlayerId,
        val existingVp: Int,
        val plantVp: Int,
        val unplayedWispVp: Int,
        val totalVp: Int,
        val graftedPlantCount: Int,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class FinalWinners(
        override val sequence: Long,
        val winnerIds: List<PlayerId>,
        override val hierarchyDepth: Int = 0
    ) : GameEntry

    data class GameCompleted(
        override val sequence: Long,
        val roundsCompleted: Int,
        override val hierarchyDepth: Int = 0
    ) : GameEntry
}
