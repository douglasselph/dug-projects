package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.game.domain.GameTime
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.local.GroveNearingTransition
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.baseline.DecisionAcquireSelectBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionBestBloomAcquisitionCardBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionDamageAbsorptionBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionDrawCountBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionFlowerSelectBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionRerollOneDieBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionShouldProcessTrashEffectBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionShouldTargetPlayerBaseline
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect
import dugsolutions.leaf.player.decisions.core.DecisionBestBloomAcquisitionCard
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.core.DecisionFlowerSelect
import dugsolutions.leaf.player.decisions.core.DecisionRerollOneDie
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.decisions.core.DecisionShouldTargetPlayer
import dugsolutions.leaf.player.decisions.local.AcquireCardEvaluator
import dugsolutions.leaf.player.decisions.local.AcquireDieEvaluator
import dugsolutions.leaf.player.di.CardEffectBattleScoreFactory

class DecisionDirector(
    private val cardEffectBattleScoreFactory: CardEffectBattleScoreFactory,
    private val cardManager: CardManager,
    private val acquireCardEvaluator: AcquireCardEvaluator,
    private val acquireDieEvaluator: AcquireDieEvaluator,
    private val groveNearingTransition: GroveNearingTransition,
    private val gameTime: GameTime
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
        shouldProcessTrashEffect = DecisionShouldProcessTrashEffectBaseline(player, groveNearingTransition, gameTime)
        shouldTargetPlayer = DecisionShouldTargetPlayerBaseline(player)
        rerollOneDie = DecisionRerollOneDieBaseline(player)
        bestBloomCardAcquisition = DecisionBestBloomAcquisitionCardBaseline()
        flowerSelectDecision = DecisionFlowerSelectBaseline(player)
    }

}
