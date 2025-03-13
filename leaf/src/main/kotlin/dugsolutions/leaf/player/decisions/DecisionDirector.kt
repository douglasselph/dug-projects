package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.player.Player

class DecisionDirector(
    player: Player,
    cardManager: CardManager
) {

    var acquireSelectDecision: DecisionAcquireSelect = DecisionAcquireSelectCoreStrategy(player)
    var drawCountDecision: DecisionDrawCount = DecisionDrawCountCoreStrategy(player)
    var damageAbsorptionDecision: DecisionDamageAbsorption = DecisionDamageAbsorptionCoreStrategy(player, cardManager)
    var shouldProcessTrashEffect: DecisionShouldProcessTrashEffect = DecisionShouldProcessTrashEffectCoreStrategy()
    var bestCardPurchase: DecisionBestCardPurchase = DecisionBestCardPurchaseCoreStrategy(player)

}
