package dugsolutions.leaf.v35.chronicle.domain

import dugsolutions.leaf.v35.battle.domain.BattleGridRowSnapshot
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter

/**
 * Input events accepted by Chronicle.
 *
 * Gameplay code records typed Moments. GameChronicle snapshots every Moment
 * into immutable GameEntry data before storing it. Marker remains available as
 * an escape hatch for diagnostic/test-only breadcrumbs, but production game
 * flow should prefer the typed vocabulary below.
 */
sealed interface Moment {

    data class Marker(val message: String) : Moment

    data class RoundRevealed(
        val roundNumber: Int,
        val cardName: String,
        val cardType: RoundCardType,
        val firstEffect: GameEffect,
        val secondEffect: GameEffect
    ) : Moment

    data class RoundCompleted(
        val roundNumber: Int,
        val cardName: String,
        val cardType: RoundCardType,
        val playerSummaries: List<PlayerRoundSummarySnapshot> = emptyList()
    ) : Moment

    data class DieRolled(
        val playerId: PlayerId,
        val sides: Int,
        val value: Int,
        val rewardPolicy: ChronicleRollRewardPolicy,
        val reason: RollReason
    ) : Moment

    data class RollReward(
        val playerId: PlayerId,
        val kind: RollRewardKind,
        val critter: Critter? = null,
        val wispName: String? = null
    ) : Moment

    data class OpeningDrawCompleted(
        val phase: ChroniclePhase,
        val playerId: PlayerId,
        val count: Int
    ) : Moment

    data class MainAction(
        val playerId: PlayerId,
        val phase: ChroniclePhase,
        val action: MainActionKind,
        val actionNumber: Int? = null,
        val battleStage: BattleMainStage? = null,
        val decisionProbabilityPercent: Int? = null
    ) : Moment {
        init {
            require(decisionProbabilityPercent == null || decisionProbabilityPercent in 0..100) {
                "Main Action decision probability must be 0..100: $decisionProbabilityPercent"
            }
        }
    }

    data class SupportAction(
        val playerId: PlayerId,
        val phase: ChroniclePhase,
        val action: SupportActionKind,
        val row: StrikeRow? = null,
        val wispUsePercentage: Int? = null
    ) : Moment

    data class EffectResolved(
        val playerId: PlayerId,
        val effect: GameEffect,
        val sourceKind: EffectSourceKind,
        val sourceName: String,
        val phase: ChroniclePhase
    ) : Moment

    /** Optional Human Baseline/debug score explanation for the selected choice. */
    data class DecisionReasoning(
        val playerId: PlayerId,
        val choiceLabel: String,
        val baseScore: Int,
        val adjustments: List<DecisionScoreAdjustmentSnapshot>,
        val total: Int
    ) : Moment

    data class BuyOrder(
        val order: List<PlayerId>,
        val resources: List<BuyOrderResourceSnapshot> = emptyList()
    ) : Moment

    data class Purchase(
        val playerId: PlayerId,
        val kind: PurchaseKind,
        val itemName: String,
        val cost: Int,
        val paymentTotal: Int
    ) : Moment

    data class Graft(
        val playerId: PlayerId,
        val plantName: String
    ) : Moment

    data class BattleOrder(
        val order: List<PlayerId>,
        val initialDiceCount: Int,
        val highestDice: List<BattleOrderHighDieSnapshot> = emptyList()
    ) : Moment

    /** Full immutable Battle Grid snapshot taken after one action rotation. */
    data class BattleGridReport(
        val kind: BattleGridReportKind,
        val passNumber: Int? = null,
        val rows: List<BattleGridRowSnapshot>
    ) : Moment

    /**
     * Battle-row context associated with one exact DieRolled Chronicle entry.
     * See GameEntry.BattleDieRow for why this is a separate typed fact.
     */
    data class BattleDieRow(
        val rollSequence: Long,
        val playerId: PlayerId,
        val sides: Int,
        val value: Int,
        val row: StrikeRow
    ) : Moment

    /** Current Butterfly ownership/facing for one player after a state change. */
    data class ButterflyState(
        val playerId: PlayerId,
        val butterflies: List<ButterflyStateSnapshot>
    ) : Moment

    data class StrikeResolved(
        val row: StrikeRow,
        val totals: List<StrikeTotalSnapshot>,
        val rowSnapshot: BattleGridRowSnapshot? = null,
        val winnerIds: List<PlayerId>,
        val woundedPlayerIds: List<PlayerId>,
        val vpPerWinner: Int
    ) : Moment

    data class Wound(
        val playerId: PlayerId,
        val kind: WoundKind,
        val plantName: String
    ) : Moment

    data class Doom(val dice: List<DoomDieSnapshot>) : Moment

    data class Refresh(val playerId: PlayerId) : Moment

    data class Cleanup(
        val playerId: PlayerId,
        val phase: ChroniclePhase,
        val discardedDice: Int,
        val returnedCritters: Int = 0,
        val refreshed: Boolean
    ) : Moment

    data class Upgrade(
        val playerId: PlayerId,
        val from: DieSides,
        val to: DieSides,
        val destination: UpgradeDestination,
        val fromValue: Int? = null
    ) : Moment

    /** A die committed to a Mulch token by an effect. */
    data class MulchStored(
        val playerId: PlayerId,
        val sides: DieSides,
        val value: Int,
        val fromDiscard: Boolean
    ) : Moment

    /** One die value mutation caused by a resolved effect. */
    data class DieValueChanged(
        val playerId: PlayerId,
        val effect: GameEffect,
        val sides: DieSides,
        val before: Int,
        val after: Int
    ) : Moment

    data class TrashDie(
        val playerId: PlayerId,
        val sides: DieSides,
        val destination: TrashDestination
    ) : Moment

    data class FinalScore(
        val playerId: PlayerId,
        val existingVp: Int,
        val plantVp: Int,
        val unplayedWispVp: Int,
        val totalVp: Int,
        val graftedPlantCount: Int
    ) : Moment

    data class FinalWinners(val winnerIds: List<PlayerId>) : Moment

    data class GameCompleted(val roundsCompleted: Int) : Moment
}

enum class ChroniclePhase { CULTIVATION, BATTLE }
enum class ChronicleRollRewardPolicy { NORMAL, IGNORE, DEFER }
enum class RollReason { DRAW, ROLL }
enum class RollRewardKind {
    IGNORED,
    CRITTER_UNAVAILABLE,
    WISP_UNAVAILABLE,
    CRITTER_GAINED,
    WISP_GAINED,
    WISP_PLAYED_IMMEDIATELY
}
enum class MainActionKind { DRAW, ACTIVATE_PLANT, ROUND_EFFECT_1, ROUND_EFFECT_2 }
enum class BattleMainStage { FIRST, FINAL }
enum class BattleGridReportKind { AFTER_FIRST_MAIN, AFTER_PASS }
enum class SupportActionKind {
    WISP,
    WATER_REROLL,
    WATER_REFRESH,
    MULCH,
    WORM_FLIP,
    BUTTERFLY,
    CRITTER_BEE,
    CRITTER_WORM
}
enum class EffectSourceKind { PLANT, ROUND, WISP }
enum class PurchaseKind { PLANT, DIE }
enum class WoundKind { FLIPPED, SNIPPED }
enum class UpgradeDestination { HAND, DISCARD }
enum class TrashDestination { OUT_OF_GAME }

data class BuyOrderDieSnapshot(
    val sides: DieSides,
    val value: Int
)

data class BuyOrderCritterSnapshot(
    val critter: Critter,
    val value: Int
)

data class BuyOrderResourceSnapshot(
    val playerId: PlayerId,
    val dice: List<BuyOrderDieSnapshot>,
    val critters: List<BuyOrderCritterSnapshot>
) {
    val total: Int
        get() = dice.sumOf { it.value } + critters.sumOf { it.value }
}

data class BattleOrderHighDieSnapshot(
    val playerId: PlayerId,
    val sides: DieSides,
    val value: Int
)

data class DecisionScoreAdjustmentSnapshot(
    val amount: Int,
    val reason: String
)

data class StrikeTotalSnapshot(
    val playerId: PlayerId,
    val diceTotal: Int,
    val critterTotal: Int,
    val total: Int
)

data class DoomDieSnapshot(
    val playerId: PlayerId,
    val row: StrikeRow,
    val sides: DieSides,
    val value: Int,
    val returnedToGraftBed: Boolean
)
