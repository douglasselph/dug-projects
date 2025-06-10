package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.cards.domain.GameCard

interface DecisionFlowerSelect {

    suspend operator fun invoke(): List<GameCard>

}
