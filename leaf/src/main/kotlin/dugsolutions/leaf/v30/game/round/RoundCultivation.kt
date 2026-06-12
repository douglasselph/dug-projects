package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.MainActionType
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.game.effect.RoundActionExecutor
import dugsolutions.leaf.v30.game.effect.WispCardEffectExecutor
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.game.effect.GameCardEffectExecutorCultivation
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.ItemsToBuy
import dugsolutions.leaf.v30.player.decision.domain.ActionCultivation
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.DieSides
import dugsolutions.leaf.v30.random.die.di.DieFactory
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.table.Table

class RoundCultivation(
    table: Table,
    card: RoundCard,
    chronicle: Chronicle = GameChronicle(),
    private val roundActionExecutor: RoundActionExecutor = RoundActionExecutor(),
    private val gameCardEffectExecutor: GameCardEffectExecutorCultivation = GameCardEffectExecutorCultivation(),
    private val wispCardEffectExecutor: WispCardEffectExecutor = WispCardEffectExecutor(),
    private val playerOrder: PlayerOrder = PlayerOrder(),
    private val dieFactory: DieFactory = DieFactory(Randomizer.create())
) : RoundBase(table, card, chronicle) {

    private companion object {
        const val ACTIONS_PER_PLAYER = 2
        const val MAX_MAIN_ACTION_ATTEMPTS = 10
    }

    override fun performMainActions() {
        table.players.forEach { player ->
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
                    actionsRemaining = actionsRemaining
                )
                if (actionSpent) {
                    actionsRemaining--
                }
            }
        }
    }

    private fun performMainAction(
        player: Player,
        actionsRemaining: Int
    ): Boolean {
        when (
            val action = player.decisionDirector.chooseMainCultivationAction(
                Decision.ChooseMainActionCultivation(
                    player = player,
                    roundCard = card,
                    table = table,
                    actionsRemaining = actionsRemaining
                )
            )
        ) {
            ActionCultivation.PullDie -> {
                val die = player.drawDiceWithRefresh().roll()
                chronicle(
                    Moment.MainAction(
                        player = player,
                        action = MainActionType.PULL_DIE,
                        detail = "Pulled and rolled a die into hand",
                        die = die
                    )
                )
                resolveReward(player, die)
            }
            is ActionCultivation.DoRoundAction -> {
                roundActionExecutor(
                    table = table,
                    player = player,
                    card = card,
                    action = action.actionRound
                )
                chronicle(
                    Moment.MainAction(
                        player = player,
                        action = MainActionType.DO_ROUND_ACTION,
                        detail = "Used cultivation round action ${action.actionRound}"
                    )
                )
            }
            is ActionCultivation.ExecuteCard -> {
                gameCardEffectExecutor(
                    table = table,
                    player = player,
                    action = action
                )
                player.flipCreatureCardFaceDown(action.card)
                chronicle(
                    Moment.MainAction(
                        player = player,
                        action = MainActionType.EXECUTE_CARD,
                        detail = "Executed a creature card and flipped it face down",
                        card = action.card
                    )
                )
                return action.usesAction
            }
            is ActionCultivation.PlayWispCard -> {
                wispCardEffectExecutor(
                    table = table,
                    player = player,
                    card = action.card
                )
                chronicle(
                    Moment.MainAction(
                        player = player,
                        action = MainActionType.PLAY_WISP_CARD,
                        detail = "Played a wisp card",
                        wispCard = action.card
                    )
                )
                return false
            }
            is ActionCultivation.PlayMulchToken -> {
                handleMulchToken(player, action.token)
                return false
            }
            is ActionCultivation.PlayWaterToken -> {
                handleWaterToken(player, action)
                return false
            }
        }
        return true
    }

    private fun handleMulchToken(
        player: Player,
        token: Token.MULCH
    ) {
        val sides = token.sides ?: throw MainActionException("Cultivation mulch token requires die sides")
        if (!player.remove(token)) return
        val die = dieFactory(sides).roll()
        player.addDieToHand(die)
        resolveReward(player, die)
        chronicle(
            Moment.MainAction(
                player = player,
                action = MainActionType.PLAY_MULCH_TOKEN,
                detail = "Played a mulch token to add a rolled die to hand",
                die = die,
                token = token
            )
        )
    }

    private fun handleWaterToken(
        player: Player,
        action: ActionCultivation.PlayWaterToken
    ) {
        val die = action.onDie
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
        if (!player.diceHand.hasDie(die)) {
            throw MainActionException("Water token die was not found in player hand")
        }
        if (!player.remove(Token.WATER)) return
        val rerolled = player.rerollDie(die) ?: throw MainActionException("Water token die was not found in player hand")
        if (rerolled != die && !player.diceHand.hasDie(rerolled)) {
            throw MainActionException("Water token die was not found in player hand")
        }
        chronicle(
            Moment.MainAction(
                player = player,
                action = MainActionType.PLAY_WATER_TOKEN,
                detail = "Played a water token to reroll a hand die",
                die = rerolled,
                token = Token.WATER
            )
        )
    }

    fun performBuy() {
        playerOrder(table.players).forEach { player ->
            val itemsToBuy = player.decisionDirector.chooseItemsToBuy(
                Decision.ChooseItemsToBuy(
                    player = player,
                    grove = table.grove
                )
            )
            buyItems(player, itemsToBuy)
            player.discardHandDice()
        }
    }

    private fun buyItems(
        player: Player,
        itemsToBuy: ItemsToBuy
    ) {
        itemsToBuy.dice.forEach { sides ->
            buyDie(player, sides)
        }
        itemsToBuy.cards.forEach { card ->
            if (table.grove.remove(card)) {
                player.addCardToCreature(card)
            }
        }
        itemsToBuy.crittersUsed.forEach { critter ->
            if (player.removeCritter(critter)) {
                table.grove.add(critter)
            }
        }
    }

    private fun buyDie(
        player: Player,
        sides: DieSides
    ) {
        if (table.grove.remove(sides)) {
            player.addDieToDiscard(dieFactory(sides))
        }
    }

}
