package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.cards.CardManager

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.baseline.DecisionAcquireSelectBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionDamageAbsorptionBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionDrawCountBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionRerollOneDieBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionShouldTargetPlayerBaseline

import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.core.DecisionRerollOneDie
import dugsolutions.leaf.player.decisions.core.DecisionShouldTargetPlayer
import dugsolutions.leaf.player.decisions.local.AcquireCardEvaluator
import dugsolutions.leaf.player.decisions.local.AcquireDieEvaluator

class DecisionDirector(
    private val cardManager: CardManager,
    private val acquireCardEvaluator: AcquireCardEvaluator,
    private val acquireDieEvaluator: AcquireDieEvaluator,
) {

    lateinit var drawCountDecision: DecisionDrawCount
    lateinit var acquireSelectDecision: DecisionAcquireSelect
    lateinit var damageAbsorptionDecision: DecisionDamageAbsorption
    lateinit var shouldTargetPlayer: DecisionShouldTargetPlayer
    lateinit var rerollOneDie: DecisionRerollOneDie

    fun initialize(player: Player) {
        drawCountDecision = DecisionDrawCountBaseline()
        acquireSelectDecision = DecisionAcquireSelectBaseline(player, acquireCardEvaluator, acquireDieEvaluator)
        damageAbsorptionDecision = DecisionDamageAbsorptionBaseline(player, cardManager)
        shouldTargetPlayer = DecisionShouldTargetPlayerBaseline(player)
        rerollOneDie = DecisionRerollOneDieBaseline(player)
    }

}
