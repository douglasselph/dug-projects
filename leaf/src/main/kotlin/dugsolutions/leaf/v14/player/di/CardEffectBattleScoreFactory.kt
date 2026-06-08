package dugsolutions.leaf.v14.player.di

import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.effect.FloralBonusCount
import dugsolutions.leaf.v14.player.decisions.local.CardEffectBattleScore
import dugsolutions.leaf.v14.player.decisions.local.EffectBattleScore

class CardEffectBattleScoreFactory(
    private val effectBattleScore: EffectBattleScore,
    private val floralBonusCount: FloralBonusCount
) {

    operator fun invoke(player: Player): CardEffectBattleScore {
        return CardEffectBattleScore(
            player,
            effectBattleScore,
            floralBonusCount
        )
    }
}
