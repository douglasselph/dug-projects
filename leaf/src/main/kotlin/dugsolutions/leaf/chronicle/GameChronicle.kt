package dugsolutions.leaf.chronicle

import dugsolutions.leaf.chronicle.domain.ChronicleEntry
import dugsolutions.leaf.chronicle.domain.TransformMomentToEntry
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.game.domain.GameTurn
import dugsolutions.leaf.game.purchase.domain.Combination
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.components.PlayersScoreData

class GameChronicle(
    private val gameTurn: GameTurn,
    private val transformMomentToEntry: TransformMomentToEntry
) {

    sealed class Moment {
        data class ACQUIRE_CARD(val player: Player, val card: GameCard, val paid: Combination): Moment()
        data class ACQUIRE_DIE(val player: Player, val die: Die, val paid: Combination): Moment()
        data class ADJUST_DIE(val player: Player, val amount: Int): Moment()
        data class ADD_TO_THORN(val player: Player, val amount: Int): Moment()

        data class ADD_TO_TOTAL(val player: Player, val amount: Int): Moment()
        data class DELIVER_DAMAGE(
            val defenders: List<Player>, val damageToDefender: Int,
            val attackers: List<Player>, val damageToAttacker: Int
        ) : Moment()
        data class DRAW_CARD(val player: Player, val cardId: CardID) : Moment()
        data class DRAW_DIE(val player: Player, val die: Die): Moment()
        data class DRAW_HAND(val player: Player) : Moment()

        data class DEFLECT_DAMAGE(val player: Player, val amount: Int): Moment()
        data class DISCARD_CARD(val player: Player, val cardId: GameCard): Moment()
        data class DISCARD_DIE(val player: Player, val die: Die): Moment()
        data class EVENT_TURN(val players: List<Player>) : Moment()
        data class EVENT_BATTLE(val result: PlayersScoreData): Moment()
        data class FINISHED(val result: PlayersScoreData) : Moment()

        data class ORDERING(val players: List<Player>, val hadReroll: Boolean) : Moment()
        data class PLAY_CARD(val player: Player, val card: GameCard): Moment()

        data class REROLL(val player: Player, val die: Die): Moment()
        data class RETAIN_CARD(val player: Player, val card: GameCard): Moment()
        data class RETAIN_DIE(val player: Player, val die: Die): Moment()
        data class REUSE_CARD(val player: Player, val card: GameCard): Moment()
        data class SET_TO_MAX(val player: Player) : Moment()
        data class TRASH_CARD(val player: Player, val cardId: CardID): Moment()
        data class TRASH_DIE(val player: Player, val die: Die): Moment()
        data class UPGRADE_DIE(val player: Player, val die: Die): Moment()

    }

    // Cache to store all chronicle entries
    private val entries = mutableListOf<ChronicleEntry>()

    /**
     * Records a game moment by transforming it into a chronicle entry and storing it.
     */
    operator fun invoke(moment: Moment) {
        // Transform the moment into a chronicle entry
        val entry = transformMomentToEntry(moment, gameTurn)
        
        // Store the entry in the cache
        entries.add(entry)
    }
    
    /**
     * Returns all recorded chronicle entries.
     */
    fun getEntries(): List<ChronicleEntry> = entries.toList()
    
    /**
     * Clears all recorded entries.
     */
    fun clear() {
        entries.clear()
    }
}
