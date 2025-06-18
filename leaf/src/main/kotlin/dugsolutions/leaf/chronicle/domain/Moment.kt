package dugsolutions.leaf.chronicle.domain

import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.domain.PlayersScoreData
import dugsolutions.leaf.random.die.DieSides

sealed class Moment {
    data class ACQUIRE_CARD(val player: Player, val card: GameCard, val paid: Combination = Combination()) : Moment()
    data class ACQUIRE_DIE(val player: Player, val die: Die, val paid: Combination) : Moment()
    data class ACQUIRE_NONE(val player: Player) : Moment()
    data class ADJUST_DIE(val player: Player, val die: Die, val amount: Int) : Moment()
    data class ADD_TO_THORN(val player: Player, val amount: Int) : Moment()

    data class ADD_TO_TOTAL(val player: Player, val amount: Int) : Moment()
    data class ADORN(val player: Player, val flowerCardId: CardID, val drawCardId: CardID) : Moment()
    data class NUTRIENT_REWARD(val player: Player, val nutrients: Int, val gained: DieSides) : Moment()
    data class DELIVER_DAMAGE(
        val defender: Player, val damageToDefender: Int, val deflectDamage: Int = 0,
        val defenderPipTotal: Int = 0, val attackerPipTotal: Int = 0
    ) : Moment()

    data class DRAW_CARD(val player: Player, val cardId: CardID) : Moment()
    data class DRAW_DIE(val player: Player, val die: Die) : Moment()
    data class DRAWN_HAND(val player: Player) : Moment()

    data class DEFLECT_DAMAGE(val player: Player, val amount: Int) : Moment()
    data class DISCARD_CARD(val player: Player, val cardId: GameCard) : Moment()
    data class DISCARD_DIE(val player: Player, val die: Die) : Moment()
    data class EVENT_TURN(val players: List<Player>) : Moment()
    data class EVENT_BATTLE_TRANSITION(val player: Player, val trashedSeedlings: List<CardID>) : Moment()
    data class FINISHED(val result: PlayersScoreData) : Moment()
    data class GAIN_D20(val player: Player) : Moment()
    data class INFO(val message: String) : Moment()

    data class ORDERING(val players: List<Player>, val numberOfRerolls: Int) : Moment()
    data class PLAY_CARD(val player: Player, val card: GameCard) : Moment()

    data class REROLL(val player: Player, val die: Die, val beforeValue: Int) : Moment()
    data class RETAIN_CARD(val player: Player, val card: GameCard) : Moment()
    data class RETAIN_DIE(val player: Player, val die: Die, val oldValue: Int? = null) : Moment()
    data class REPLAY_VINE(val player: Player, val selectedVine: GameCard) : Moment()
    data class REPORT(val line: String): Moment()
    data class REUSE_CARD(val player: Player, val card: GameCard) : Moment()
    data class REUSE_DIE(val player: Player, val die: Die) : Moment()
    data class SET_TO_MAX(val player: Player) : Moment()
    data class TRASH_CARD(val player: Player, val card: GameCard, val floralArray: Boolean = false) : Moment()
    data class TRASH_DIE(val player: Player, val die: Die) : Moment()
    data class TRASH_FOR_EFFECT(val player: Player, val card: GameCard, val status: DecisionShouldProcessTrashEffect.Result) : Moment()
    data class THORN_DAMAGE(val player: Player, val thornDamage: Int) : Moment()
    data class UPGRADE_DIE(val player: Player, val die: Die) : Moment()
    data class USE_FLOWERS(val player: Player, val flowers: List<GameCard>): Moment()
    data class USE_OPPONENT_CARD(val player: Player, val card: GameCard): Moment()
    data class USE_OPPONENT_DIE(val player: Player, val die: Die): Moment()
}
