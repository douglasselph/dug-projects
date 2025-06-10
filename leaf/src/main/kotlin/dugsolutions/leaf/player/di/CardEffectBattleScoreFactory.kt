package dugsolutions.leaf.player.di

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.components.FloralBonusCount
import dugsolutions.leaf.player.decisions.local.CardEffectBattleScore
import dugsolutions.leaf.player.decisions.local.EffectBattleScore

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
