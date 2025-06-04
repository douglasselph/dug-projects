package dugsolutions.leaf.di.factory

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.decisions.local.AcquireCardEvaluator
import dugsolutions.leaf.player.decisions.local.AcquireDieEvaluator
import dugsolutions.leaf.player.decisions.local.GroveNearingTransition

class DecisionDirectorFactory(
    private val cardManager: CardManager,
    private val cardEffectBattleScoreFactory: CardEffectBattleScoreFactory,
    private val acquireCardEvaluator: AcquireCardEvaluator,
    private val acquireDieEvaluator: AcquireDieEvaluator,
    private val groveNearingTransition: GroveNearingTransition
) {

    operator fun invoke(player: Player): DecisionDirector {
        return DecisionDirector(
            player,
            cardEffectBattleScoreFactory,
            cardManager,
            acquireCardEvaluator,
            acquireDieEvaluator,
            groveNearingTransition
        )
    }

}
