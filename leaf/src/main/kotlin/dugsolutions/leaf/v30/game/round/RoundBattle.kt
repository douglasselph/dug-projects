package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.domain.BattleGridSnapshot
import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.game.effect.WispCardEffectExecutor
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.MainAction
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.di.DieFactory
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.table.Table

class RoundBattle(
    table: Table,
    card: RoundCard,
    chronicle: Chronicle = GameChronicle(),
    private val battle: Battle = table.battle,
    private val wispCardEffectExecutor: WispCardEffectExecutor = WispCardEffectExecutor(),
    private val dieFactory: DieFactory = DieFactory(Randomizer.create())
) : RoundBase(table, card, chronicle) {

    private companion object {
        const val ACTIONS_PER_PLAYER = 2
        const val MAX_MAIN_ACTION_ATTEMPTS = 10
    }

    fun prepare() {
        battle.setup(table.players)
    }

    override fun performMainActions() {
        val snapshot = battle.snapshot()
        val playersById = table.players.associateBy { it.id }
        snapshot.playerIdsInGridOrder.asReversed().forEach { playerId ->
            playersById[playerId]?.let { player ->
                var actionsRemaining = ACTIONS_PER_PLAYER
                var attempts = 0
                while (actionsRemaining > 0) {
                    attempts++
                    if (attempts > MAX_MAIN_ACTION_ATTEMPTS) {
                        throw MainActionException(
                            "Exceeded $MAX_MAIN_ACTION_ATTEMPTS main action attempts for player ${player.id}"
                        )
                    }
                    val actionSpent = performMainAction(
                        player = player,
                        snapshot = battle.snapshot(),
                        actionsRemaining = actionsRemaining
                    )
                    if (actionSpent) {
                        actionsRemaining--
                    }
                }
            }
        }
    }

    private fun performMainAction(
        player: Player,
        snapshot: BattleGridSnapshot,
        actionsRemaining: Int
    ): Boolean {
        when (
            val action = player.decisionDirector.chooseMainActionBattle(
                Decision.ChooseMainActionBattle(
                    player = player,
                    roundCard = card,
                    table = table,
                    battleGridSnapshot = snapshot,
                    actionsRemaining = actionsRemaining
                )
            )
        ) {
            MainAction.PullDie -> player.drawDiceWithRefresh()
            is MainAction.DoRoundAction -> {
            }
            is MainAction.ExecuteCard -> {
            }
            is MainAction.DoWispCard -> {
                wispCardEffectExecutor(
                    table = table,
                    player = player,
                    card = action.card
                )
                return false
            }
            is MainAction.PlayMulchToken -> {
                handleMulchToken(player, action)
                return false
            }
            is MainAction.PlayWaterToken -> {
                handleWaterToken(player, action)
                return false
            }
        }
        return true
    }

    private fun handleMulchToken(
        player: Player,
        action: MainAction.PlayMulchToken
    ) {
        val sides = action.token.sides ?: throw MainActionException("Battle mulch token requires die sides")
        val row = action.row ?: throw MainActionException("Battle mulch token requires a battle row")
        val die = dieFactory(sides).roll()
        if (!battle.grid.getSquare(player.id, row).canAdd(BattleItem.DieItem(die))) {
            return
        }
        if (!player.remove(action.token)) return
        battle.add(player, row, die)
        resolveReward(player, die)
    }

    private fun handleWaterToken(
        player: Player,
        action: MainAction.PlayWaterToken
    ) {
        val die = action.onDie
        val row = action.row
        if ((die == null) != (row == null)) {
            throw MainActionException("Battle water token requires both die and row, or neither")
        }
        if (die == null && row == null) {
            if (!player.remove(Token.WATER)) return
            player.flipAllCreatureCardsFaceUp()
            return
        }
        val targetRow = row ?: throw MainActionException("Battle water token missing row")
        val targetDie = die ?: throw MainActionException("Battle water token missing die")
        if (!battle.hasDie(player, targetRow, targetDie)) {
            throw MainActionException("Water token die was not found in battle grid")
        }
        if (!player.remove(Token.WATER)) return
        if (!battle.rerollDie(player, targetRow, targetDie)) {
            throw MainActionException("Water token die was not found in battle grid")
        }
    }

    fun performSupportActions() {

    }
}
