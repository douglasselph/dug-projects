package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.BattleAwardWinners
import dugsolutions.leaf.v30.battle.domain.BattleGridSnapshot
import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.MainActionType
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.Moment.*
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.game.effect.GameCardEffectExecutorBattle
import dugsolutions.leaf.v30.game.effect.WispCardEffectExecutor
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.ActionBattleMain
import dugsolutions.leaf.v30.player.decision.domain.ActionBattleSupport
import dugsolutions.leaf.v30.player.decision.domain.Decision.*
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.di.DieFactory
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.table.Table

class RoundBattle(
    table: Table,
    card: RoundCard,
    chronicle: Chronicle = GameChronicle(),
    private val battle: Battle = table.battle,
    private val battleAwardWinners: BattleAwardWinners = BattleAwardWinners(chronicle),
    private val gameCardEffectExecutor: GameCardEffectExecutorBattle = GameCardEffectExecutorBattle(),
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
            val action = player.decisionDirector.chooseMainBattleAction(
                ChooseMainActionBattle(
                    player = player,
                    roundCard = card,
                    table = table,
                    battleGridSnapshot = snapshot,
                    actionsRemaining = actionsRemaining
                )
            )
        ) {
            is ActionBattleMain.PullDie -> {
                val die = player.drawDiceWithRefresh().roll()
                chronicle(
                    MainAction(
                        player = player,
                        action = MainActionType.PULL_DIE,
                        detail = "Pulled and rolled a die and added it to strike row ${action.row}",
                        die = die
                    )
                )
                resolveReward(player, die)
                battle.add(player, action.row, die)
            }
            is ActionBattleMain.DoRoundAction -> {
                chronicle(
                    MainAction(
                        player = player,
                        action = MainActionType.DO_ROUND_ACTION,
                        detail = "Used battle round action ${action.actionRound}"
                    )
                )
            }
            is ActionBattleMain.ExecuteCard -> {
                gameCardEffectExecutor(
                    table = table,
                    player = player,
                    action = action
                )
                player.flipCreatureCardFaceDown(action.card)
                chronicle(
                    MainAction(
                        player = player,
                        action = MainActionType.EXECUTE_CARD,
                        detail = "Executed a battle card effect",
                        card = action.card
                    )
                )
            }
            is ActionBattleMain.PlayWispCard -> {
                wispCardEffectExecutor(
                    table = table,
                    player = player,
                    card = action.card
                )
                chronicle(
                    MainAction(
                        player = player,
                        action = MainActionType.PLAY_WISP_CARD,
                        detail = "Played a wisp card during battle",
                        wispCard = action.card
                    )
                )
                return false
            }
        }
        return true
    }

    private fun performSupportAction(
        player: Player,
        snapshot: BattleGridSnapshot,
        actionsRemaining: Int
    ): Boolean {
        when (
            val action = player.decisionDirector.chooseSupportBattleAction(
                Decision.ChooseMainActionBattle(
                    player = player,
                    roundCard = card,
                    table = table,
                    battleGridSnapshot = snapshot,
                    actionsRemaining = actionsRemaining
                )
            )
        ) {
            is ActionBattleSupport.PlayWispCard -> {
                wispCardEffectExecutor(
                    table = table,
                    player = player,
                    card = action.card
                )
                chronicle(
                    Moment.MainAction(
                        player = player,
                        action = MainActionType.PLAY_WISP_CARD,
                        detail = "Played a wisp card during battle",
                        wispCard = action.card
                    )
                )
            }
            is ActionBattleSupport.PlayMulchToken -> {
                handleMulchToken(player, action)
            }
            is ActionBattleSupport.PlayWaterToken -> {
                handleWaterToken(player, action)
            }
            ActionBattleSupport.None -> return false
        }
        return true
    }

    fun resolve() {
        val result = battle.computeWinners()
        battleAwardWinners(table.players, result)
    }

    private fun handleMulchToken(
        player: Player,
        action: ActionBattleSupport.PlayMulchToken
    ) {
        val sides = action.token.sides ?: throw MainActionException("Battle mulch token requires die sides")
        val row = action.row
        val die = dieFactory(sides).roll()
        if (!battle.grid.getSquare(player.id, row).canAdd(BattleItem.DieItem(die))) {
            return
        }
        if (!player.remove(action.token)) return
        battle.add(player, row, die)
        resolveReward(player, die)
        chronicle(
            Moment.MainAction(
                player = player,
                action = MainActionType.PLAY_MULCH_TOKEN,
                detail = "Played a mulch token to add a rolled die to battle row $row",
                die = die,
                token = action.token
            )
        )
    }

    private fun handleWaterToken(
        player: Player,
        action: ActionBattleSupport.PlayWaterToken
    ) {
        val die = action.onDie
        val row = action.row
        if ((die == null) != (row == null)) {
            throw MainActionException("Battle water token requires both die and row, or neither")
        }
        if (die == null) {
            if (!player.remove(Token.WATER)) return
            player.flipAllCreatureCardsFaceUp()
            chronicle(
                Moment.MainAction(
                    player = player,
                    action = MainActionType.PLAY_WATER_TOKEN,
                    detail = "Played a water token to refresh all creature cards",
                    token = Token.WATER
                )
            )
            return
        }
        val targetRow = row ?: throw MainActionException("Battle water token missing row")
        val targetDie = die
        if (!battle.hasDie(player, targetRow, targetDie)) {
            throw MainActionException("Water token die was not found in battle grid")
        }
        if (!player.remove(Token.WATER)) return
        val rerolled = battle.rerollDie(player, targetRow, targetDie)
            ?: throw MainActionException("Water token die was not found in battle grid")
        if (rerolled != targetDie && !battle.hasDie(player, targetRow, rerolled)) {
            throw MainActionException("Water token die was not found in battle grid")
        }
        chronicle(
            Moment.MainAction(
                player = player,
                action = MainActionType.PLAY_WATER_TOKEN,
                detail = "Played a water token to reroll a battle die in row $targetRow",
                die = rerolled,
                token = Token.WATER
            )
        )
    }

    fun performSupportActions() {
        val snapshot = battle.snapshot()
        val playersById = table.players.associateBy { it.id }
        snapshot.playerIdsInGridOrder.asReversed().forEach { playerId ->
            playersById[playerId]?.let { player ->
                var attempts = 0
                while (true) {
                    attempts++
                    if (attempts > MAX_MAIN_ACTION_ATTEMPTS) {
                        throw MainActionException(
                            "Exceeded $MAX_MAIN_ACTION_ATTEMPTS support action attempts for player ${player.id}"
                        )
                    }
                    val actionTaken = performSupportAction(
                        player = player,
                        snapshot = battle.snapshot(),
                        actionsRemaining = ACTIONS_PER_PLAYER
                    )
                    if (!actionTaken) return@let
                }
            }
        }
    }
}
