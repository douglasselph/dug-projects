package dugsolutions.leaf.di.factory

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.components.FloralCount
import dugsolutions.leaf.player.decisions.local.CardEffectBattleScore
import dugsolutions.leaf.player.decisions.local.EffectBattleScore

class CardEffectBattleScoreFactory(
    private val effectBattleScore: EffectBattleScore,
    private val floralCount: FloralCount
) {

    operator fun invoke(player: Player): CardEffectBattleScore {
        return CardEffectBattleScore(
            player,
            effectBattleScore,
            floralCount
        )
    }
}
