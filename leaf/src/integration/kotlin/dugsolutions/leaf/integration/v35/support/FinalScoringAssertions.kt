package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.game.scoring.FinalScoringResult
import dugsolutions.leaf.v35.game.scoring.PlayerFinalScore
import dugsolutions.leaf.v35.player.PlayerId
import kotlin.test.assertEquals

/** Readable assertions for exact-state final-scoring integration scenarios. */
object FinalScoringAssertions {

    fun scoreFor(
        result: FinalScoringResult,
        playerId: Int
    ): PlayerFinalScore =
        result.scores.single { it.playerId == PlayerId(playerId) }

    fun assertScore(
        result: FinalScoringResult,
        playerId: Int,
        existingVp: Int,
        plantVp: Int,
        unplayedWispVp: Int,
        totalVp: Int,
        graftedPlantCount: Int
    ) {
        val score = scoreFor(result, playerId)
        assertEquals(existingVp, score.existingVp, "P$playerId existing VP")
        assertEquals(plantVp, score.plantVp, "P$playerId Plant VP")
        assertEquals(unplayedWispVp, score.unplayedWispVp, "P$playerId unplayed Wisp VP")
        assertEquals(totalVp, score.totalVp, "P$playerId total VP")
        assertEquals(graftedPlantCount, score.graftedPlantCount, "P$playerId grafted Plant count")
    }

    fun assertWinners(
        result: FinalScoringResult,
        vararg playerIds: Int
    ) {
        assertEquals(
            playerIds.map(::PlayerId),
            result.winnerIds,
            "final winner IDs"
        )
    }
}
