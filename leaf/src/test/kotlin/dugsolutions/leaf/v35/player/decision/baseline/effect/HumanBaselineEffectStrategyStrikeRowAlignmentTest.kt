package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectStrikeRowRequest
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineEffectStrategyStrikeRowAlignmentTest {
    private val p1 = PlayerId(1)
    private val p2 = PlayerId(2)

    @Test
    fun `Root and Scoot withdraws from wounded losing row instead of first legal winning row`() {
        val context = context(
            rows = listOf(
                row(
                    StrikeRow.TOP,
                    player(p1, dieTotal = 10, dice = listOf(die(0, 12, 10))),
                    player(p2, dieTotal = 8, dice = listOf(die(0, 10, 8)))
                ),
                row(
                    StrikeRow.MIDDLE,
                    player(p1, dieTotal = 4, dice = listOf(die(1, 6, 4))),
                    player(p2, dieTotal = 10, dice = listOf(die(1, 12, 10)))
                )
            )
        )

        val chosen = HumanBaselineEffectStrategy().chooseStrikeRow(
            ChooseEffectStrikeRowRequest(
                effect = GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE,
                legalChoices = listOf(StrikeRow.TOP, StrikeRow.MIDDLE),
                context = context
            )
        )

        assertEquals(StrikeRow.MIDDLE, chosen)
    }

    @Test
    fun `Vine and Punishment chooses second row where exact opposing reductions flip the Strike`() {
        val context = context(
            rows = listOf(
                row(
                    StrikeRow.TOP,
                    player(p1, dieTotal = 5, dice = listOf(die(0, 6, 5))),
                    player(
                        p2,
                        dieTotal = 6,
                        critterTotal = 4,
                        dice = listOf(die(0, 6, 6))
                    )
                ),
                row(
                    StrikeRow.MIDDLE,
                    player(p1, dieTotal = 8, dice = listOf(die(1, 8, 8))),
                    player(
                        p2,
                        dieTotal = 4,
                        critterTotal = 6,
                        dice = listOf(die(1, 6, 4))
                    )
                )
            )
        )

        val chosen = HumanBaselineEffectStrategy().chooseStrikeRow(
            ChooseEffectStrikeRowRequest(
                effect = GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3,
                legalChoices = listOf(StrikeRow.TOP, StrikeRow.MIDDLE),
                context = context
            )
        )

        assertEquals(StrikeRow.MIDDLE, chosen)
    }


    @Test
    fun `Gust of Petals chooses second row when expected opposing reroll flips that Strike`() {
        val context = context(
            rows = listOf(
                row(
                    StrikeRow.TOP,
                    player(p1, dieTotal = 10, dice = listOf(die(0, 12, 10))),
                    player(p2, dieTotal = 9, dice = listOf(die(0, 20, 9)))
                ),
                row(
                    StrikeRow.MIDDLE,
                    player(p1, dieTotal = 11, dice = listOf(die(1, 12, 11))),
                    player(p2, dieTotal = 12, dice = listOf(die(1, 20, 12)))
                )
            )
        )

        val chosen = HumanBaselineEffectStrategy().chooseStrikeRow(
            ChooseEffectStrikeRowRequest(
                effect = GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW,
                legalChoices = listOf(StrikeRow.TOP, StrikeRow.MIDDLE),
                context = context
            )
        )

        assertEquals(StrikeRow.MIDDLE, chosen)
    }

    @Test
    fun `Gust of Petals avoids row where expected opposing reroll turns a win into a loss`() {
        val context = context(
            rows = listOf(
                row(
                    StrikeRow.TOP,
                    player(
                        p1,
                        dieTotal = 10,
                        dice = listOf(die(0, 12, 1), die(1, 12, 9))
                    ),
                    player(p2, dieTotal = 2, dice = listOf(die(0, 20, 2)))
                ),
                row(
                    StrikeRow.MIDDLE,
                    player(p1, dieTotal = 10, dice = listOf(die(2, 12, 10))),
                    player(p2, dieTotal = 9, dice = listOf(die(1, 20, 9)))
                )
            )
        )

        val chosen = HumanBaselineEffectStrategy().chooseStrikeRow(
            ChooseEffectStrikeRowRequest(
                effect = GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW,
                legalChoices = listOf(StrikeRow.TOP, StrikeRow.MIDDLE),
                context = context
            )
        )

        assertEquals(StrikeRow.MIDDLE, chosen)
    }

    private fun context(rows: List<BattleRowView>): DecisionContext =
        DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(id = p1)
            ),
            opponents = listOf(
                OpponentView(
                    board = DecisionContext.EMPTY.self.board.copy(id = p2),
                    wispCount = 0
                )
            ),
            battle = BattleView(
                playerOrder = listOf(p1, p2),
                rows = rows
            )
        )

    private fun row(row: StrikeRow, vararg players: BattlePlayerRowView) =
        BattleRowView(
            row = row,
            closed = false,
            players = players.map { it.copy(row = row) }
        )

    private fun player(
        id: PlayerId,
        dieTotal: Int,
        critterTotal: Int = 0,
        dice: List<BattleDieView>
    ) = BattlePlayerRowView(
        playerId = id,
        row = StrikeRow.TOP,
        dice = dice,
        critters = emptyList(),
        dieTotal = dieTotal,
        critterTotal = critterTotal,
        total = dieTotal + critterTotal,
        withdrawn = false
    )

    private fun die(index: Int, sides: Int, value: Int) =
        BattleDieView(handIndex = index, sides = sides, value = value)
}
