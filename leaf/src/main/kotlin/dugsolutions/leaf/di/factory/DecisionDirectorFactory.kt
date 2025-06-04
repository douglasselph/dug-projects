package dugsolutions.leaf.di.factory

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.DecisionDirector

class DecisionDirectorFactory(
    private val cardManager: CardManager,
    private val cardEffectBattleScoreFactory: CardEffectBattleScoreFactory
) {

    operator fun invoke(player: Player): DecisionDirector {
        return DecisionDirector(player, cardEffectBattleScoreFactory, cardManager)
    }

}
