package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.di.factory.CardEffectBattleScoreFactory
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.baseline.DecisionAcquireSelectBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionBestBloomCardBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionDamageAbsorptionBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionDrawCountBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionRerollOneDieBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionShouldProcessTrashEffectBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionShouldTargetPlayerBaseline
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect
import dugsolutions.leaf.player.decisions.core.DecisionBestBloomCard
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.core.DecisionRerollOneDie
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.decisions.core.DecisionShouldTargetPlayer
import dugsolutions.leaf.player.decisions.local.AcquireCardEvaluator
import dugsolutions.leaf.player.decisions.local.AcquireDieEvaluator
import dugsolutions.leaf.player.decisions.local.GroveNearingTransition

class DecisionDirector(
    player: Player,
    cardEffectBattleScoreFactory: CardEffectBattleScoreFactory,
    cardManager: CardManager,
    acquireCardEvaluator: AcquireCardEvaluator,
    acquireDieEvaluator: AcquireDieEvaluator,
    groveNearingTransition: GroveNearingTransition
) {

    var drawCountDecision: DecisionDrawCount = DecisionDrawCountBaseline(player)
    var acquireSelectDecision: DecisionAcquireSelect = DecisionAcquireSelectBaseline(player, acquireCardEvaluator, acquireDieEvaluator)
    var damageAbsorptionDecision: DecisionDamageAbsorption = DecisionDamageAbsorptionBaseline(player, cardEffectBattleScoreFactory, cardManager)
    var shouldProcessTrashEffect: DecisionShouldProcessTrashEffect = DecisionShouldProcessTrashEffectBaseline(groveNearingTransition)
    var shouldTargetPlayer: DecisionShouldTargetPlayer = DecisionShouldTargetPlayerBaseline(player)
    var rerollOneDie: DecisionRerollOneDie = DecisionRerollOneDieBaseline(player)
    var bestBloomCard: DecisionBestBloomCard = DecisionBestBloomCardBaseline()

}
