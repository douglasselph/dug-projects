package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.baseline.DecisionAcquireSelectBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionBestBloomCardBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionBestCardPurchaseBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionDamageAbsorptionBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionDrawCountBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionRerollOneDieBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionShouldProcessTrashEffectBaseline
import dugsolutions.leaf.player.decisions.baseline.DecisionShouldTargetPlayerBaseline
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect
import dugsolutions.leaf.player.decisions.core.DecisionBestBloomCard
import dugsolutions.leaf.player.decisions.core.DecisionBestCardPurchase
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.core.DecisionRerollOneDie
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.decisions.core.DecisionShouldTargetPlayer

class DecisionDirector(
    player: Player,
    cardManager: CardManager
) {

    var bestCardPurchase: DecisionBestCardPurchase = DecisionBestCardPurchaseBaseline(player)
    var acquireSelectDecision: DecisionAcquireSelect = DecisionAcquireSelectBaseline(player)
    var drawCountDecision: DecisionDrawCount = DecisionDrawCountBaseline(player)
    var damageAbsorptionDecision: DecisionDamageAbsorption = DecisionDamageAbsorptionBaseline(player, cardManager)
    var shouldProcessTrashEffect: DecisionShouldProcessTrashEffect = DecisionShouldProcessTrashEffectBaseline()
    var shouldTargetPlayer: DecisionShouldTargetPlayer = DecisionShouldTargetPlayerBaseline(player)
    var rerollOneDie: DecisionRerollOneDie = DecisionRerollOneDieBaseline(player)
    var bestBloomCard: DecisionBestBloomCard = DecisionBestBloomCardBaseline()

}
