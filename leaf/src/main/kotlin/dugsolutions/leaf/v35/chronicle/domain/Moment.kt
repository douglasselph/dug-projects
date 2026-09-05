package dugsolutions.leaf.v35.chronicle.domain

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
        val cardType: RoundCardType
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
        val battleStage: BattleMainStage? = null
    ) : Moment

    data class SupportAction(
        val playerId: PlayerId,
        val phase: ChroniclePhase,
        val action: SupportActionKind,
        val row: StrikeRow? = null
    ) : Moment

    data class EffectResolved(
        val playerId: PlayerId,
        val effect: GameEffect,
        val sourceKind: EffectSourceKind,
        val sourceName: String,
        val phase: ChroniclePhase
    ) : Moment

    data class BuyOrder(val order: List<PlayerId>) : Moment

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
        val initialDiceCount: Int
    ) : Moment

    data class StrikeResolved(
        val row: StrikeRow,
        val totals: List<StrikeTotalSnapshot>,
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
        val destination: UpgradeDestination
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
enum class ChronicleRollRewardPolicy { NORMAL, IGNORE }
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
