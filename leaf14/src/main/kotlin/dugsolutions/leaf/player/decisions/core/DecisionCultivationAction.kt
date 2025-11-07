package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.common.domain.Action
import dugsolutions.leaf.common.domain.Token
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides

interface DecisionCultivationAction {

    suspend operator fun invoke(
        availableCards: List<GameCard>,
        availableDice: List<DieSides>,
        availableBugs: List<Token>
    ): Action

}
