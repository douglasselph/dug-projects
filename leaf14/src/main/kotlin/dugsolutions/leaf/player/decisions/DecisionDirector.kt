package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.cards.CardManager

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.baseline.DecisionCultivationActionBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionDamageAbsorptionBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionDrawCountBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionRerollOneDieBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionShouldTargetPlayerBaseline

import dugsolutions.leaf.player.decisions.core.DecisionCultivationAction
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.core.DecisionRerollOneDie
import dugsolutions.leaf.player.decisions.core.DecisionShouldTargetPlayer

class DecisionDirector(
    private val cardManager: CardManager
) {

    lateinit var drawCountDecision: DecisionDrawCount
    lateinit var cultivationAction: DecisionCultivationAction
    lateinit var damageAbsorptionDecision: DecisionDamageAbsorption
    lateinit var shouldTargetPlayer: DecisionShouldTargetPlayer
    lateinit var rerollOneDie: DecisionRerollOneDie

    fun initialize(player: Player) {
        drawCountDecision = DecisionDrawCountBaseline()
        cultivationAction = DecisionCultivationActionBaseline(player)
        damageAbsorptionDecision = DecisionDamageAbsorptionBaseline(player, cardManager)
        shouldTargetPlayer = DecisionShouldTargetPlayerBaseline(player)
        rerollOneDie = DecisionRerollOneDieBaseline(player)
    }

}
