package dugsolutions.leaf.player.decisions.baseline


import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.common.domain.Action
import dugsolutions.leaf.common.domain.Token
import dugsolutions.leaf.common.domain.game.GamePhase
import dugsolutions.leaf.common.evaluator.GenerateCombinations
import dugsolutions.leaf.common.evaluator.PossibleCardsToAcquire
import dugsolutions.leaf.common.evaluator.PossibleDiceToAcquire
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionCultivationAction
import dugsolutions.leaf.player.decisions.local.EvaluateAcquireBug
import dugsolutions.leaf.player.decisions.local.EvaluateAcquireCard
import dugsolutions.leaf.player.decisions.local.EvaluateAcquireDie
import dugsolutions.leaf.player.decisions.local.EvaluatePullDie
import dugsolutions.leaf.player.decisions.local.EvaluateExecuteCard
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.die.DieValues

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
        
        // Order of checks:
        // 1. evaluatePullDie
        val pullDieAction = checkPullDie()
        if (pullDieAction != null) return pullDieAction
        
        // 2. evaluateExecuteCard for resource cards
        val executeResourceAction = checkExecuteResourceCards(resourceCardsInHand, ownedBugs, diceInHand.dice)
        if (executeResourceAction != Action.None) return executeResourceAction
        
        // 3. evaluateExecuteCard for creature cards
        val executeCreatureAction = checkExecuteCreatureCards(cardsNeedingSap, hasSap, ownedBugs, diceInHand.dice)
        if (executeCreatureAction != Action.None) return executeCreatureAction
        
        // 4. evaluateAcquireBug
        val acquireBugAction = checkAcquireBug(availableBugs, ownedBugs)
        if (acquireBugAction != Action.None) return acquireBugAction
        
        // 5. evaluateAcquireCard
        val acquireCardAction = checkAcquireCard(availableCards, cultivationCardsInDiscard, diceInHand)
        if (acquireCardAction != Action.None) return acquireCardAction
        
        // 6. evaluateAcquireDie (last, uses combinations already generated)
        val acquireDieAction = checkAcquireDie(availableDice, diceInHand)
        if (acquireDieAction != Action.None) return acquireDieAction
        
        return Action.None
    }

    /**
     * Determines which cards on the creature can still be executed.
     * A card can be executed if there are more copies on the creature than have been executed.
     * For example, if a player has 2 copies of a card on the creature and only 1 has been executed,
     * then that card can still be executed.
     */
    private fun cardsStillPossible(cardsOnCreature: List<GameCard>, cardsAlreadyExecuted: List<GameCard>): List<GameCard> {
        // Count how many times each card appears on the creature
        val creatureCardCounts = cardsOnCreature.groupingBy { it.id }.eachCount()
        
        // Count how many times each card has been executed
        val executedCardCounts = cardsAlreadyExecuted.groupingBy { it.id }.eachCount()
        
        // Build list of cards that can still be executed
        val result = mutableListOf<GameCard>()
        for (card in cardsOnCreature) {
            val creatureCount = creatureCardCounts[card.id] ?: 0
            val executedCount = executedCardCounts[card.id] ?: 0
            
            // If there are more copies on creature than executed, this card can still be executed
            if (creatureCount > executedCount) {
                result.add(card)
            }
        }
        
        return result
    }

    /**
     * Checks if player wants to pull a die from their creature.
     */
    private fun checkPullDie(): Action.PullDie? {
        val die = evaluatePullDie(player)
        return die?.let { Action.PullDie(it) }
    }

    /**
     * Checks if player wants to execute a resource card from hand.
     */
    private fun checkExecuteResourceCards(
        resourceCardsInHand: List<GameCard>,
        ownedBugs: List<Token>,
        diceToApplyTo: List<dugsolutions.leaf.random.die.Die>
    ): Action {
        if (resourceCardsInHand.isEmpty()) {
            return Action.None
        }
        
        val action = evaluateExecuteCard(resourceCardsInHand, ownedBugs, diceToApplyTo)
        return if (action != Action.None) action else Action.None
    }

    /**
     * Checks if player wants to execute a creature card that needs sap.
     */
    private fun checkExecuteCreatureCards(
        cardsNeedingSap: List<GameCard>,
        hasSap: Boolean,
        ownedBugs: List<Token>,
        diceToApplyTo: List<dugsolutions.leaf.random.die.Die>
    ): Action {
        if (!hasSap || cardsNeedingSap.isEmpty()) {
            return Action.None
        }
        
        val action = evaluateExecuteCard(cardsNeedingSap, ownedBugs, diceToApplyTo)
        return if (action != Action.None) action else Action.None
    }

    /**
     * Checks if player wants to acquire a bug.
     * Only returns Action.AcquireBug if:
     * - Player has at least one die with value 1
     * - Player doesn't already have 3 of each insect
     */
    private fun checkAcquireBug(
        availableBugs: List<Token>,
        ownedBugs: List<Token>
    ): Action {
        // Check if player has at least one die with value 1
        val hasDieWithValue1 = player.diceInHand.dice.any { it.value == 1 }
        if (!hasDieWithValue1) {
            return Action.None
        }
        
        // Check if player already has 3 of each insect
        val bugCounts = ownedBugs.groupingBy { it }.eachCount()
        val hasThreeOfEach = bugCounts.values.all { it >= 3 }
        if (hasThreeOfEach) {
            return Action.None
        }
        
        // Call evaluator to determine which bugs to acquire
        val acquireCount = 1 // Assuming 1 bug per die with value 1
        val bugsToAcquire = evaluateAcquireBug(ownedBugs, availableBugs, acquireCount, GamePhase.CULTIVATION)
        
        // If evaluator returns bugs, we need to create an Action.AcquireBug
        // But we need a DieValue with value 1 to use
        val dieWithValue1 = player.diceInHand.dice.firstOrNull { it.value == 1 }
        if (dieWithValue1 != null && bugsToAcquire.isNotEmpty()) {
            val bugToAcquire = bugsToAcquire.first()
            return Action.AcquireBug(bugToAcquire, dieWithValue1.copy)
        }
        
        return Action.None
    }

    /**
     * Checks if player wants to acquire a card.
     * Main determination: if cultivationCardsInDiscard is zero, player will try to acquire.
     */
    private fun checkAcquireCard(
        availableCards: List<GameCard>,
        cultivationCardsInDiscard: List<GameCard>,
        diceInHand: dugsolutions.leaf.random.die.Dice
    ): Action {
        // If there are cultivation cards in discard, don't acquire
        if (cultivationCardsInDiscard.isNotEmpty()) {
            return Action.None
        }
        
        // Generate combinations from dice in hand
        val dieValues = DieValues.from(diceInHand.dice)
        val combinations = generateCombinations(dieValues)
        
        // Get possible cards to acquire
        val possibleChoices = possibleCardsToAcquire(combinations, availableCards)
        
        // Evaluate which card to acquire
        val choice = evaluateAcquireCard(player, possibleChoices)
        
        if (choice != null) {
            return Action.AcquireCard(choice.card, choice.usingDice.dice)
        }
        
        return Action.None
    }

    /**
     * Checks if player wants to acquire a die.
     * Uses combinations already generated from diceInHand.
     */
    private fun checkAcquireDie(
        availableDice: List<DieSides>,
        diceInHand: dugsolutions.leaf.random.die.Dice
    ): Action {
        // Generate combinations from dice in hand
        val dieValues = DieValues.from(diceInHand.dice)
        val combinations = generateCombinations(dieValues)
        
        // Get possible dice choices based on combinations and available dice
        val possibleChoices = possibleDiceToAcquire(combinations, availableDice)
        
        // Evaluate which die to acquire
        val choice = evaluateAcquireDie(possibleChoices)
        
        if (choice != null) {
            return Action.AcquireDie(choice.dieSides, choice.usingDice.dice)
        }
        
        return Action.None
    }

} 
