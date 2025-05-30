package dugsolutions.leaf.chronicle.domain

import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.components.die.DieValues
import dugsolutions.leaf.game.acquire.domain.Combination

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

data class AdjustDieEntry(
    override val playerId: Int,
    override val turn: Int,
    val die: DieValue,
    val adjustment: Int
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
    val amount: Int
) : ChronicleEntry(playerId, turn)

data class AdornEntry(
    override val playerId: Int,
    override val turn: Int,
    val cardId: CardID
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

data class DrawHandEntry(
    override val playerId: Int,
    override val turn: Int,
    val cards: List<String>,
    val dice: DieValues
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
    val dieSides: Int
) : ChronicleEntry(playerId, turn)

data class ScoreInfo(
    val data: PlayerScore
)

data class EventTurn(
    override val turn: Int,
    val reports: List<String>,
    val scores: List<ScoreInfo>
) : ChronicleEntry(0, turn) {
    override fun toString(): String {
        return "=== Turn $turn ===\n" + reports.joinToString("\n")
    }
}

data class EventBattle(
    override val turn: Int,
    val scores: List<ScoreInfo>
) : ChronicleEntry(0, turn) {
    override fun toString(): String {
        return "+++ Battle Begins on Turn $turn +++"
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
    val newValue: Int
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

data class ReuseCardEntry(
    override val playerId: Int,
    override val turn: Int,
    val cardId: CardID,
    val cardName: String
) : ChronicleEntry(playerId, turn)

data class TrashCardEntry(
    override val playerId: Int,
    override val turn: Int,
    val card: GameCard
) : ChronicleEntry(playerId, turn)

data class TrashDieEntry(
    override val playerId: Int,
    override val turn: Int,
    val dieSides: Int
) : ChronicleEntry(playerId, turn)

data class UpgradeDieEntry(
    override val playerId: Int,
    override val turn: Int,
    val newSides: Int
) : ChronicleEntry(playerId, turn)
