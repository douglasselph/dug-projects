package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.common.evaluator.GenerateCombinations
import dugsolutions.leaf.common.evaluator.PossibleCardsToAcquire
import dugsolutions.leaf.common.evaluator.PossibleDiceToAcquire
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.baseline.DecisionBattleInsectBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionCultivationActionBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionDamageAbsorptionBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionDrawCountBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionGraftCardBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionRerollOneDieBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionShouldTargetPlayerBaseline
import dugsolutions.leaf.player.decisions.core.DecisionBattleInsect
import dugsolutions.leaf.player.decisions.core.DecisionCultivationAction
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.core.DecisionGraftCard
import dugsolutions.leaf.player.decisions.core.DecisionRerollOneDie
import dugsolutions.leaf.player.decisions.core.DecisionShouldTargetPlayer
import dugsolutions.leaf.player.decisions.local.EvaluateAcquireBug
import dugsolutions.leaf.player.decisions.local.EvaluateAcquireCard
import dugsolutions.leaf.player.decisions.local.EvaluateAcquireDie
import dugsolutions.leaf.player.decisions.local.EvaluateExecuteCard
import dugsolutions.leaf.player.decisions.local.EvaluatePullDie

class DecisionDirector(
    private val cardManager: CardManager,
    private val evaluateAcquireBug: EvaluateAcquireBug,
    private val evaluateAcquireCard: EvaluateAcquireCard,
    private val evaluateAcquireDie: EvaluateAcquireDie,
    private val evaluatePullDie: EvaluatePullDie,
    private val evaluateExecuteCard: EvaluateExecuteCard,
    private val generateCombinations: GenerateCombinations,
    private val possibleCardsToAcquire: PossibleCardsToAcquire,
    private val possibleDiceToAcquire: PossibleDiceToAcquire
) {

    lateinit var drawCountDecision: DecisionDrawCount
    lateinit var cultivationAction: DecisionCultivationAction
    lateinit var damageAbsorptionDecision: DecisionDamageAbsorption
    lateinit var shouldTargetPlayer: DecisionShouldTargetPlayer
    lateinit var rerollOneDie: DecisionRerollOneDie
    lateinit var battleInsect: DecisionBattleInsect
    lateinit var graftCard: DecisionGraftCard

    fun initialize(player: Player) {
        drawCountDecision = DecisionDrawCountBaseline(player)
        cultivationAction = DecisionCultivationActionBaseline(
            player,
            evaluateAcquireBug,
            evaluateAcquireCard,
            evaluateAcquireDie,
            evaluatePullDie,
            evaluateExecuteCard,
            generateCombinations,
            possibleCardsToAcquire,
            possibleDiceToAcquire
        )
        damageAbsorptionDecision = DecisionDamageAbsorptionBaseline(player, cardManager)
        shouldTargetPlayer = DecisionShouldTargetPlayerBaseline(player)
        rerollOneDie = DecisionRerollOneDieBaseline(player)
        battleInsect = DecisionBattleInsectBaseline(player)
        graftCard = DecisionGraftCardBaseline(player)
    }

}
