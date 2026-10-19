package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.common.domain.Token
import dugsolutions.leaf.common.domain.game.GamePhase


class EvaluateAcquireBug {

    operator fun invoke(
        hasBugs: List<Token>,
        possibleBugs: List<Token>,
        acquireCount: Int,
        phase: GamePhase
    ): List<Token> {
        // During cultivation: worm (if none), bee (if none), ladybug (if worm, bee >= 2), aphid (if everything else >= 2)
        // During battle: aphid, (ladybug|bee), then worm.
        return emptyList()
    }
}
