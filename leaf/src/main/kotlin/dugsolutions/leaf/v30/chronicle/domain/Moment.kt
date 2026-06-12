package dugsolutions.leaf.v30.chronicle.domain

import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.domain.CreatureCard
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.wisp.domain.WispCard

sealed class Moment {
    data class Warning(
        val player: Player,
        val type: WarningType,
        val card: GameCard? = null,
        val actualCount: Int? = null
    ) : Moment()

    data class LoadingWarning(
        val name: String,
        val title: String,
        val reason: String
    ) : Moment()

    data class RoundRevealed(
        val card: RoundCard
    ) : Moment()

    data class DiceRolled(
        val player: Player
    ) : Moment()

    data class Reward(
        val player: Player,
        val die: Die,
        val critter: Critter? = null,
        val wispCard: WispCard? = null,
        val token: Token? = null
    ) : Moment()

    data class MainAction(
        val player: Player,
        val action: MainActionType,
        val detail: String,
        val die: Die? = null,
        val token: Token? = null,
        val card: GameCard? = null,
        val wispCard: WispCard? = null
    ) : Moment()

    data class GameCardEffect(
        val player: Player,
        val card: GameCard,
        val effect: CardEffect,
        val detail: String,
        val dice: Dice? = null,
        val token: Token? = null,
        val critter: Critter? = null
    ) : Moment()

    data class VpAward(
        val player: Player,
        val row: BattleStrikeRow,
        val amount: Int
    ) : Moment()

    data class WoundCard(
        val player: Player,
        val card: CreatureCard,
        val wasFlipped: Boolean,
        val wasLost: Boolean
    ) : Moment()
}

enum class MainActionType {
    PULL_DIE,
    DO_ROUND_ACTION,
    EXECUTE_CARD,
    PLAY_WISP_CARD,
    PLAY_MULCH_TOKEN,
    PLAY_WATER_TOKEN
}
