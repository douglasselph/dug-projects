package dugsolutions.leaf.integration.v35.sanity.experiment

import dugsolutions.leaf.integration.v35.support.GameScenario
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.RollRewardKind
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.intervention.CultivationOpeningDrawFaceIntervention
import dugsolutions.leaf.v35.game.intervention.MechanicalIntervention
import dugsolutions.leaf.v35.game.intervention.MechanicalInterventionFactory
import dugsolutions.leaf.v35.game.intervention.MechanicalRollInterventionRequest
import dugsolutions.leaf.v35.game.intervention.MechanicalRollSource
import dugsolutions.leaf.v35.player.PlayerId
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SixWispOpeningInterventionIntegrationTest {

    @Test
    fun `affected player gets six opening twos through real roll reward path then intervention stops`() {
        val affected = PlayerId(1)
        lateinit var recording: RecordingIntervention

        val scenario = GameScenario(
            numPlayers = 4,
            roundSetup = GameRoundSetup.Ordered(
                cultivationRounds = 3,
                battleRounds = 0
            ),
            exactRoundNames = listOf(
                "Resource_Sunlight_Water",
                "Resource_Water_Mulch",
                "Resource_Compost_Mulch"
            ),
            seed = 20260925L,
            strategySeed = 20260925L,
            decisionFactories = List(4) { PlayerDecisionFactory.humanBaseline() },
            mechanicalInterventionFactory = MechanicalInterventionFactory {
                RecordingIntervention(
                    CultivationOpeningDrawFaceIntervention(
                        affectedPlayerId = affected,
                        firstCultivationRounds = 2,
                        forcedFace = 2
                    )
                ).also { recording = it }
            }
        )

        IntegrationGameHarness(scenario).use { harness ->
            harness.runGame()

            val openingRequests = recording.calls.filter {
                it.request.source == MechanicalRollSource.CULTIVATION_OPENING_DRAW
            }
            assertEquals(36, openingRequests.size) // 4 players × 3 dice × 3 rounds.

            val affectedOpening = openingRequests.filter { it.request.playerId == affected }
            assertEquals(9, affectedOpening.size)
            assertEquals(List(6) { 2 }, affectedOpening.take(6).map { it.replacement })
            assertTrue(affectedOpening.drop(6).all { it.replacement == null })
            assertTrue(openingRequests.filter { it.request.playerId != affected }.all { it.replacement == null })

            // Natural values exist before replacement; the intervention does not generate the roll.
            assertTrue(affectedOpening.take(6).all { it.request.naturalValue in 1..it.request.sides })
            assertTrue(affectedOpening.take(6).any { it.request.naturalValue != 2 })

            val entries = harness.game.chronicle.entries
            val firstTwoAffectedOpenings = openingWindows(entries, affected).take(2)
            assertEquals(2, firstTwoAffectedOpenings.size)
            firstTwoAffectedOpenings.forEach { window ->
                val rolls = window.filterIsInstance<GameEntry.DieRolled>()
                    .filter { it.playerId == affected }
                assertEquals(3, rolls.size)
                assertEquals(listOf(2, 2, 2), rolls.map { it.value })

                val wispRewards = window.filterIsInstance<GameEntry.RollReward>()
                    .filter {
                        it.playerId == affected &&
                            it.kind in setOf(
                                RollRewardKind.WISP_GAINED,
                                RollRewardKind.WISP_PLAYED_IMMEDIATELY
                            )
                    }
                assertEquals(3, wispRewards.size)
            }

            val thirdOpening = openingWindows(entries, affected)[2]
                .filterIsInstance<GameEntry.DieRolled>()
                .filter { it.playerId == affected }
            assertEquals(3, thirdOpening.size)
            // The third opening is natural, not another forced 2, for this deterministic seed.
            assertNotEquals(listOf(2, 2, 2), thirdOpening.map { it.value })
        }
    }

    private fun openingWindows(
        entries: List<GameEntry>,
        playerId: PlayerId
    ): List<List<GameEntry>> {
        val windows = mutableListOf<List<GameEntry>>()
        var roundStart = -1
        entries.forEachIndexed { index, entry ->
            if (entry is GameEntry.RoundRevealed) {
                roundStart = index
            }
            if (entry is GameEntry.OpeningDrawCompleted && entry.playerId == playerId) {
                check(roundStart >= 0)
                windows += entries.subList(roundStart, index + 1)
                roundStart = index + 1
            }
        }
        return windows
    }

    private class RecordingIntervention(
        private val delegate: MechanicalIntervention
    ) : MechanicalIntervention {
        val calls = mutableListOf<Call>()

        override fun replacementFor(request: MechanicalRollInterventionRequest): Int? {
            val replacement = delegate.replacementFor(request)
            calls += Call(request, replacement)
            return replacement
        }
    }

    private data class Call(
        val request: MechanicalRollInterventionRequest,
        val replacement: Int?
    )
}
