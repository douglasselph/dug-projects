package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BattleTempoAssessorTest {
    private val actor = PlayerId(0)
    private val opponent = PlayerId(1)
    private val assessor = BattleTempoAssessor()

    @Test
    fun `close live contest compares normalized support capacity`() {
        val result = assessor(
            context = context(actorTotal = 8, opponentTotal = 10, actorBees = 2, opponentBees = 1),
            legalChoices = listOf(bee(), finalMain()),
            finalMains = listOf(BattleMainAction.RoundEffect1)
        )

        assertEquals(BattleTempoPressure.STRONG, result.pressure)
        assertEquals(setOf(opponent), result.relevantOpponentIds)
    }

    @Test
    fun `Done score benchmark does not create tempo pressure`() {
        val result = assessor(
            context = context(
                actorTotal = 8,
                opponentTotal = 10,
                actorBees = 2,
                opponentBees = 3,
                done = setOf(opponent)
            ),
            legalChoices = listOf(bee(), finalMain()),
            finalMains = listOf(BattleMainAction.RoundEffect1)
        )

        assertEquals(BattleTempoPressure.NONE, result.pressure)
    }

    private fun context(
        actorTotal: Int,
        opponentTotal: Int,
        actorBees: Int,
        opponentBees: Int,
        done: Set<PlayerId> = emptySet()
    ): DecisionContext {
        val opponentBoard = DecisionContext.EMPTY.self.board.copy(id = opponent, bees = opponentBees)
        return DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(id = actor, bees = actorBees)
            ),
            opponents = listOf(OpponentView(opponentBoard, wispCount = 0)),
            battle = BattleView(
                playerOrder = listOf(actor, opponent),
                donePlayerIds = done,
                rows = listOf(
                    BattleRowView(
                        row = StrikeRow.TOP,
                        closed = false,
                        players = listOf(
                            playerRow(actor, actorTotal),
                            playerRow(opponent, opponentTotal)
                        )
                    )
                )
            )
        )
    }

    private fun playerRow(id: PlayerId, total: Int) =
        BattlePlayerRowView(id, StrikeRow.TOP, emptyList(), emptyList(), total, 0, total, false)

    private fun bee(): BattleTurnAction =
        BattleTurnAction.Support(BattleSupportAction.PlaceCritter(Critter.BEE, StrikeRow.TOP))

    private fun finalMain(): BattleTurnAction =
        BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1)
}
