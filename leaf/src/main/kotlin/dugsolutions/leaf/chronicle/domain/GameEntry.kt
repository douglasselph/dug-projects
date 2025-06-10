package dugsolutions.leaf.chronicle.domain

import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.DieValue
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.random.die.Die

/**
 * Base class for all chronicle entries.
 * Contains common properties shared by all entries.
 */
abstract class ChronicleEntry(
    open val playerId: Int,
    open val turn: Int
)

/**
 * Data classes for each type of game moment.
 * These represent the internal storage format of game events.
 */

data class AcquireCardEntry(
    override val playerId: Int,
    override val turn: Int,
    val cardId: CardID,
    val cardName: String,
    val paid: Combination
) : ChronicleEntry(playerId, turn)

data class AcquireDieEntry(
    override val playerId: Int,
    override val turn: Int,
    val dieSides: Int,
    val paid: Combination
) : ChronicleEntry(playerId, turn)

data class AcquireNone(
    override val playerId: Int,
    override val turn: Int,
    val diceTotal: Int,
    val pipModifier: Int,
    val effects: String
) : ChronicleEntry(playerId, turn)

data class AdjustDieEntry(
    override val playerId: Int,
    override val turn: Int,
    val die: DieValue,
    val adjustment: Int,
    val dice: String
) : ChronicleEntry(playerId, turn)

data class AdjustDieToMax(
    override val playerId: Int,
    override val turn: Int,
) : ChronicleEntry(playerId, turn)

data class AddToThornEntry(
    override val playerId: Int,
    override val turn: Int,
    val amount: Int
) : ChronicleEntry(playerId, turn)

data class AddToTotalEntry(
    override val playerId: Int,
    override val turn: Int,
    val amount: Int,
    val pips: Int
) : ChronicleEntry(playerId, turn)

data class AdornEntry(
    override val playerId: Int,
    override val turn: Int,
    val flowerCardId: CardID,
    val drawCardId: CardID
) : ChronicleEntry(playerId, turn)

data class DeliverDamageEntry(
    override val playerId: Int,
    override val turn: Int,
    val report: String
) : ChronicleEntry(playerId, turn)

data class DrawCardEntry(
    override val playerId: Int,
    override val turn: Int,
    val cardId: CardID,
    val cardName: String
) : ChronicleEntry(playerId, turn)

data class DrawDieEntry(
    override val playerId: Int,
    override val turn: Int,
    val dieSides: Int
) : ChronicleEntry(playerId, turn)

data class DrawnHandEntry(
    override val playerId: Int,
    override val turn: Int,
    val cards: List<String>,
    val dice: String
) : ChronicleEntry(playerId, turn) {
    override fun toString(): String {
        return "DrawHandEntry(playerId=$playerId, turn=$turn, cards=$cards, dice=$dice)"
    }
}

data class DeflectDamageEntry(
    override val playerId: Int,
    override val turn: Int,
    val amount: Int
) : ChronicleEntry(playerId, turn)

data class DiscardCardEntry(
    override val playerId: Int,
    override val turn: Int,
    val cardId: CardID,
    val cardName: String
) : ChronicleEntry(playerId, turn)

data class DiscardDieEntry(
    override val playerId: Int,
    override val turn: Int,
    val die: DieValue
) : ChronicleEntry(playerId, turn)

data class ScoreInfo(
    val data: PlayerScore
)

data class EventTurn(
    override val turn: Int,
    val gamePhase: GamePhase,
    val reports: List<String>,
    val scores: List<ScoreInfo>
) : ChronicleEntry(0, turn) {
    override fun toString(): String {
        val phase = if (gamePhase == GamePhase.CULTIVATION) "Cultivation" else "Battle"
        return "=== $phase Turn $turn ===\n" + reports.joinToString("\n")
    }
}

data class EventBattleTransition(
    override val turn: Int,
    val score: PlayerScore,
    val trashedSeedlings: List<String>
) : ChronicleEntry(score.playerId, turn) {
    override fun toString(): String {
        val trashed = trashedSeedlings.joinToString(",")
        val trashedReport = if (trashed.isEmpty()) ": No seedlings" else ": Trashed $trashed"
        return "+++ Battle Begins Player $playerId $trashedReport +++"
    }
}

data class Finished(
    override val turn: Int,
    val reports: List<String>,
    val scores: List<ScoreInfo>
) : ChronicleEntry(0, turn) {
    override fun toString(): String {
        return "Game Over ${turn}\n" + reports.joinToString("\n")
    }
}

data class InfoEntry(
    override val turn: Int,
    val message: String
) : ChronicleEntry(0, turn)

data class OrderingEntry(
    override val playerId: Int = 0,
    override val turn: Int,
    val playerOrder: List<Int>,
    val reports: List<String>,
    val numberRerolls: Int = 0
) : ChronicleEntry(playerId, turn) {
    override fun toString(): String {
        val buffer = StringBuffer()
        if (reports.isNotEmpty()) {
            buffer.append("Reordered on turn $turn: $playerOrder")
            for (report in reports) {
                buffer.append("\n    $report")
            }
        } else {
            buffer.append("Order on turn $turn: $playerOrder")
        }
        return buffer.toString()
    }
}

data class PlayCardEntry(
    override val playerId: Int,
    override val turn: Int,
    val cardId: CardID,
    val cardName: String
) : ChronicleEntry(playerId, turn)

data class RerollEntry(
    override val playerId: Int,
    override val turn: Int,
    val dieSides: Int,
    val before: Int,
    val newValue: Int,
    val dice: String
) : ChronicleEntry(playerId, turn)

data class RetainCardEntry(
    override val playerId: Int,
    override val turn: Int,
    val cardId: CardID,
    val cardName: String
) : ChronicleEntry(playerId, turn)

data class RetainDieEntry(
    override val playerId: Int,
    override val turn: Int,
    val dieSides: Int
) : ChronicleEntry(playerId, turn)

data class ReplayVineEntry(
    override val playerId: Int,
    override val turn: Int,
    val vineId: CardID,
    val vineName: String
) : ChronicleEntry(playerId, turn)

data class ReuseCardEntry(
    override val playerId: Int,
    override val turn: Int,
    val cardId: CardID,
    val cardName: String
) : ChronicleEntry(playerId, turn)

data class ReuseDieEntry(
    override val playerId: Int,
    override val turn: Int,
    val die: DieValue,
) : ChronicleEntry(playerId, turn)

data class TrashCardEntry(
    override val playerId: Int,
    override val turn: Int,
    val card: GameCard
) : ChronicleEntry(playerId, turn) {
    override fun toString(): String {
        return "TrashCardEntry(card=${card.id}, playerId=$playerId, turn=$turn)"
    }
}

data class TrashDieEntry(
    override val playerId: Int,
    override val turn: Int,
    val dieSides: Int
) : ChronicleEntry(playerId, turn)

data class TrashForEffect(
    override val playerId: Int,
    override val turn: Int,
    val card: String,
    val status: DecisionShouldProcessTrashEffect.Result
) : ChronicleEntry(playerId, turn)

data class UpgradeDieEntry(
    override val playerId: Int,
    override val turn: Int,
    val newSides: Int
) : ChronicleEntry(playerId, turn)

data class UseOpponentCardEntry(
    override val playerId: Int,
    override val turn: Int,
    val cardId: CardID,
    val cardName: String
) : ChronicleEntry(playerId, turn)

data class UseOpponentDieEntry(
    override val playerId: Int,
    override val turn: Int,
    val die: DieValue
) : ChronicleEntry(playerId, turn)
