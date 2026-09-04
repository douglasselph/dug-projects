package dugsolutions.leaf.v35.game.scoring

import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId

/** Final VP breakdown for one player. */
data class PlayerFinalScore(
    val playerId: PlayerId,
    /** VP already earned during play and represented by VP tokens/state. */
    val existingVp: Int,
    val plantVp: Int,
    val unplayedWispVp: Int,
    val totalVp: Int,
    /** Used only as the first tiebreaker after total VP. */
    val graftedPlantCount: Int
)

/**
 * Complete end-game scoring result.
 *
 * [winnerIds] contains one player when the Plant-count tiebreaker resolves the
 * tie, or multiple players when the title remains shared after that tiebreaker.
 */
data class FinalScoringResult(
    val scores: List<PlayerFinalScore>,
    val winnerIds: List<PlayerId>
)

/** Implements the v35 final scoring and winner/tiebreak rules. */
class FinalScorer {

    fun score(game: Game): FinalScoringResult {
        val scores = game.players.map(::scorePlayer)
        if (scores.isEmpty()) {
            return FinalScoringResult(
                scores = emptyList(),
                winnerIds = emptyList()
            )
        }

        val highestVp = scores.maxOf { it.totalVp }
        val vpLeaders = scores.filter { it.totalVp == highestVp }
        val highestPlantCount = vpLeaders.maxOf { it.graftedPlantCount }
        val winners = vpLeaders
            .filter { it.graftedPlantCount == highestPlantCount }
            .map { it.playerId }

        return FinalScoringResult(
            scores = scores,
            winnerIds = winners
        )
    }

    private fun scorePlayer(player: Player): PlayerFinalScore {
        val plantVp = player.creature.cards.sumOf { creatureCard ->
            when (val rule = creatureCard.card.scoringRule) {
                is PlantScoringRule.Fixed -> rule.points
                PlantScoringRule.PerGraftedVine -> player.creature.vines.size
                PlantScoringRule.PerButterfly -> player.butterflies.size
            }
        }

        val wispVp = player.wisps.sumOf { it.endGameVp }
        val total = player.vp + plantVp + wispVp

        return PlayerFinalScore(
            playerId = player.id,
            existingVp = player.vp,
            plantVp = plantVp,
            unplayedWispVp = wispVp,
            totalVp = total,
            graftedPlantCount = player.creature.size
        )
    }
}
