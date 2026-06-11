package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.game.effect.GameCardEffectExecutor
import dugsolutions.leaf.v30.game.effect.RoundActionExecutor
import dugsolutions.leaf.v30.game.effect.WispCardEffectExecutor
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.ItemsToBuy
import dugsolutions.leaf.v30.player.decision.domain.MainAction
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
    private val gameCardEffectExecutor: GameCardEffectExecutor = GameCardEffectExecutor(),
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
            val action = player.decisionDirector.chooseMainActionCultivation(
                Decision.ChooseMainActionCultivation(
                    player = player,
                    roundCard = card,
                    table = table,
                    actionsRemaining = actionsRemaining
                )
            )
        ) {
            MainAction.PullDie -> player.drawDiceWithRefresh()
            is MainAction.DoRoundAction -> {
                roundActionExecutor(
                    table = table,
                    player = player,
                    card = card,
                    action = action.roundAction
                )
            }
            is MainAction.ExecuteCard -> {
                gameCardEffectExecutor(
                    table = table,
                    player = player,
                    action = action
                )
                player.flipCreatureCardFaceDown(action.card)
            }
            is MainAction.DoWispCard -> {
                wispCardEffectExecutor(
                    table = table,
                    player = player,
                    card = action.card
                )
                return false
            }
        }
        return true
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
            player.removeCritter(critter)
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
