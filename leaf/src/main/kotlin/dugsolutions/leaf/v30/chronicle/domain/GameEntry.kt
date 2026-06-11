package dugsolutions.leaf.v30.chronicle.domain

import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.cards.domain.GameCardID
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.player.domain.CreatureCard
import dugsolutions.leaf.v30.random.die.DieValue
import dugsolutions.leaf.v30.round.domain.RoundCardID
import dugsolutions.leaf.v30.round.domain.RoundCardType
import dugsolutions.leaf.v30.wisp.domain.WispCardID

sealed class GameEntry(
    open val sequence: Long,
    open val time: GameTimeSnapshot,
    open val kind: EntryKind,
    open val playerId: Int
) {
    data class Warning(
        override val sequence: Long,
        override val time: GameTimeSnapshot,
        override val playerId: Int,
        val type: WarningType,
        val cardId: GameCardID?,
        val cardName: String?,
        val actualCount: Int? = null
    ) : GameEntry(
        sequence = sequence,
        time = time,
        kind = EntryKind.WARNING,
        playerId = playerId
    )

    data class LoadingWarning(
        override val sequence: Long,
        override val time: GameTimeSnapshot,
        val name: String,
        val title: String,
        val reason: String
    ) : GameEntry(
        sequence = sequence,
        time = time,
        kind = EntryKind.LOADING_WARNING,
        playerId = 0
    )

    data class RoundRevealed(
        override val sequence: Long,
        override val time: GameTimeSnapshot,
        val roundCardId: RoundCardID,
        val roundCardName: String,
        val roundCardTitle: String,
        val roundCardType: RoundCardType
    ) : GameEntry(
        sequence = sequence,
        time = time,
        kind = EntryKind.ROUND_REVEALED,
        playerId = 0
    )

    data class DiceRolled(
        override val sequence: Long,
        override val time: GameTimeSnapshot,
        override val playerId: Int,
        val dice: List<DieValue>
    ) : GameEntry(
        sequence = sequence,
        time = time,
        kind = EntryKind.DICE_ROLLED,
        playerId = playerId
    )

    data class Reward(
        override val sequence: Long,
        override val time: GameTimeSnapshot,
        override val playerId: Int,
        val die: DieValue,
        val critter: Critter? = null,
        val wispCardId: WispCardID? = null,
        val wispCardName: String? = null,
        val wispCardTitle: String? = null,
        val token: Token? = null
    ) : GameEntry(
        sequence = sequence,
        time = time,
        kind = EntryKind.REWARD,
        playerId = playerId
    )

    data class MainAction(
        override val sequence: Long,
        override val time: GameTimeSnapshot,
        override val playerId: Int,
        val action: MainActionType,
        val detail: String,
        val die: DieValue? = null,
        val token: Token? = null,
        val cardId: GameCardID? = null,
        val cardName: String? = null,
        val wispCardId: WispCardID? = null,
        val wispCardName: String? = null
    ) : GameEntry(
        sequence = sequence,
        time = time,
        kind = EntryKind.MAIN_ACTION,
        playerId = playerId
    )

    data class GameCardEffect(
        override val sequence: Long,
        override val time: GameTimeSnapshot,
        override val playerId: Int,
        val cardId: GameCardID,
        val cardName: String,
        val effect: CardEffect,
        val detail: String,
        val die: DieValue? = null,
        val token: Token? = null,
        val critter: Critter? = null
    ) : GameEntry(
        sequence = sequence,
        time = time,
        kind = EntryKind.GAME_CARD_EFFECT,
        playerId = playerId
    )

    data class VpAward(
        override val sequence: Long,
        override val time: GameTimeSnapshot,
        override val playerId: Int,
        val row: BattleStrikeRow,
        val amount: Int
    ) : GameEntry(
        sequence = sequence,
        time = time,
        kind = EntryKind.VP_AWARD,
        playerId = playerId
    )

    data class WoundCard(
        override val sequence: Long,
        override val time: GameTimeSnapshot,
        override val playerId: Int,
        val cardId: GameCardID,
        val cardName: String,
        val facingBefore: CreatureCard.Facing,
        val wasFlipped: Boolean,
        val wasLost: Boolean
    ) : GameEntry(
        sequence = sequence,
        time = time,
        kind = EntryKind.WOUND_CARD,
        playerId = playerId
    )
}
