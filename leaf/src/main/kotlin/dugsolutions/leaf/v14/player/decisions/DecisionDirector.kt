package dugsolutions.leaf.v14.player.decisions

import dugsolutions.leaf.v14.cards.CardManager
import dugsolutions.leaf.v14.grove.local.GroveNearingTransition
import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.decisions.baseline.DecisionAcquireSelectBaseline
import dugsolutions.leaf.v14.player.decisions.baseline.DecisionBestBloomAcquisitionCardBaseline
import dugsolutions.leaf.v14.player.decisions.baseline.DecisionDamageAbsorptionBaseline
import dugsolutions.leaf.v14.player.decisions.baseline.DecisionDrawCountBaseline
import dugsolutions.leaf.v14.player.decisions.baseline.DecisionFlowerSelectBaseline
import dugsolutions.leaf.v14.player.decisions.baseline.DecisionRerollOneDieBaseline
import dugsolutions.leaf.v14.player.decisions.baseline.DecisionShouldProcessTrashEffectBaseline
import dugsolutions.leaf.v14.player.decisions.baseline.DecisionShouldTargetPlayerBaseline
import dugsolutions.leaf.v14.player.decisions.core.DecisionAcquireSelect
import dugsolutions.leaf.v14.player.decisions.core.DecisionBestBloomAcquisitionCard
import dugsolutions.leaf.v14.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.v14.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.v14.player.decisions.core.DecisionFlowerSelect
import dugsolutions.leaf.v14.player.decisions.core.DecisionRerollOneDie
import dugsolutions.leaf.v14.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.v14.player.decisions.core.DecisionShouldTargetPlayer
import dugsolutions.leaf.v14.player.decisions.local.AcquireCardEvaluator
import dugsolutions.leaf.v14.player.decisions.local.AcquireDieEvaluator
import dugsolutions.leaf.v14.player.di.CardEffectBattleScoreFactory

class DecisionDirector(
    private val cardEffectBattleScoreFactory: CardEffectBattleScoreFactory,
    private val cardManager: CardManager,
    private val acquireCardEvaluator: AcquireCardEvaluator,
    private val acquireDieEvaluator: AcquireDieEvaluator,
    private val groveNearingTransition: GroveNearingTransition
) {

    lateinit var drawCountDecision: DecisionDrawCount
    lateinit var acquireSelectDecision: DecisionAcquireSelect
    lateinit var damageAbsorptionDecision: DecisionDamageAbsorption
    lateinit var shouldProcessTrashEffect: DecisionShouldProcessTrashEffect
    lateinit var shouldTargetPlayer: DecisionShouldTargetPlayer
    lateinit var rerollOneDie: DecisionRerollOneDie
    lateinit var bestBloomCardAcquisition: DecisionBestBloomAcquisitionCard
    lateinit var flowerSelectDecision: DecisionFlowerSelect

    fun initialize(player: Player) {
        drawCountDecision = DecisionDrawCountBaseline()
        acquireSelectDecision = DecisionAcquireSelectBaseline(player, acquireCardEvaluator, acquireDieEvaluator)
        damageAbsorptionDecision = DecisionDamageAbsorptionBaseline(player, cardEffectBattleScoreFactory, cardManager)
        shouldProcessTrashEffect = DecisionShouldProcessTrashEffectBaseline(groveNearingTransition)
        shouldTargetPlayer = DecisionShouldTargetPlayerBaseline(player)
        rerollOneDie = DecisionRerollOneDieBaseline(player)
        bestBloomCardAcquisition = DecisionBestBloomAcquisitionCardBaseline()
        flowerSelectDecision = DecisionFlowerSelectBaseline(player)
    }

    val usingBaselineDrawCount: Boolean
        get() = drawCountDecision is DecisionDrawCountBaseline

}
