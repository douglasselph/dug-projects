package dugsolutions.leaf.player.decisions.baseline


import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.common.domain.Action
import dugsolutions.leaf.common.domain.Token
import dugsolutions.leaf.game.acquire.evaluator.GenerateCombinations
import dugsolutions.leaf.game.acquire.evaluator.PossibleCardsToAcquire
import dugsolutions.leaf.game.acquire.evaluator.PossibleDiceToAcquire
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionCultivationAction
import dugsolutions.leaf.player.decisions.local.EvaluateAcquireBug
import dugsolutions.leaf.player.decisions.local.EvaluateAcquireCard
import dugsolutions.leaf.player.decisions.local.EvaluateAcquireDie
import dugsolutions.leaf.player.decisions.local.EvaluatePullDie
import dugsolutions.leaf.player.decisions.local.EvaluateExecuteCard
import dugsolutions.leaf.random.die.DieSides

class DecisionCultivationActionBaseline(
    private val player: Player,
    private val evaluateAcquireBug: EvaluateAcquireBug,
    private val evaluateAcquireCard: EvaluateAcquireCard,
    private val evaluateAcquireDie: EvaluateAcquireDie,
    private val evaluatePullDie: EvaluatePullDie,
    private val evaluateExecuteCard: EvaluateExecuteCard,
    private val generateCombinations: GenerateCombinations,
    private val possibleCardsToAcquire: PossibleCardsToAcquire,
    private val possibleDiceToAcquire: PossibleDiceToAcquire
) : DecisionCultivationAction {

    override suspend fun invoke(availableCards: List<GameCard>, availableDice: List<DieSides>, availableBugs: List<Token>): Action {
        val diceInHand = player.diceInHand
        val cultivationCardsInDiscard = player.cardsInDiscard().filter { it.type != FlourishType.RESOURCE }
        val cardsAlreadyExecuted = player.cardsExecuted
        val cardsOnCreature = player.cardsOnCreature
        val cardsStillPossible = cardsStillPossible(cardsOnCreature, cardsAlreadyExecuted)
        val cardsNeedingSap =
            cardsStillPossible.filter { it.matchWith == MatchWith.Sap || it.matchWith == MatchWith.WormOrSap }
        val resourceCardsInHand = player.cardsInHand().filter { it.type == FlourishType.RESOURCE }
        val hasSap = player.hasSap
        val ownedBugs = player.insects()
        return Action.None
    }

    private fun cardsStillPossible(cardsOnCreature: List<GameCard>, cardsAlreadyExecuted: List<GameCard>): List<GameCard> {
        return cardsOnCreature
    }

} 
