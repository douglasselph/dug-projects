package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.player.decision.effect.*
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineEffectStrategyBattleSpecialTest {
    private val p1 = PlayerId(1)
    private val p2 = PlayerId(2)

    @Test
    fun `Pollen Theft chooses the best complete shared-analysis swap`() {
        val context = context(
            rows = listOf(
                row(StrikeRow.TOP, player(p1, 4, die(0, 6, 4)), player(p2, 5, die(0, 6, 5))),
                row(StrikeRow.MIDDLE, player(p1, 10, die(1, 6, 6)), player(p2, 2, die(1, 6, 2)))
            )
        )
        val good = EffectCrossPlayerDieSwapChoice(
            EffectBattleDieChoice(p1, StrikeRow.TOP, EffectDieChoice(0, 6, 4)),
            EffectBattleDieChoice(p2, StrikeRow.TOP, EffectDieChoice(0, 6, 5))
        )
        val harmful = EffectCrossPlayerDieSwapChoice(
            EffectBattleDieChoice(p1, StrikeRow.MIDDLE, EffectDieChoice(1, 6, 6)),
            EffectBattleDieChoice(p2, StrikeRow.MIDDLE, EffectDieChoice(1, 6, 2))
        )

        val chosen = HumanBaselineEffectStrategy().chooseCrossPlayerDieSwap(
            ChooseEffectCrossPlayerDieSwapRequest(
                effect = GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE,
                legalChoices = listOf(harmful, good),
                context = context
            )
        )

        assertEquals(good, chosen)
    }

    @Test
    fun `Wisp Resolve chooses a row meeting the approved lock-in policy over one that misses lead threshold`() {
        val context = context(
            rows = listOf(
                row(StrikeRow.TOP, player(p1, 10), player(p2, 5)),
                row(StrikeRow.MIDDLE, player(p1, 9), player(p2, 5))
            ),
            opponentWisps = 3
        )

        val chosen = HumanBaselineEffectStrategy().chooseStrikeRow(
            ChooseEffectStrikeRowRequest(
                effect = GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW,
                legalChoices = listOf(StrikeRow.MIDDLE, StrikeRow.TOP),
                context = context
            )
        )

        assertEquals(StrikeRow.TOP, chosen)
    }

    @Test
    fun `Overgrowth maximizes resulting die size even when smaller result has stronger immediate Battle swing`() {
        val context = context(
            rows = listOf(
                row(StrikeRow.TOP, player(p1, 1, die(0, 4, 1)), player(p2, 2)),
                row(StrikeRow.MIDDLE, player(p1, 6, die(1, 6, 6)), player(p2, 20))
            ),
            graftBed = mapOf(DieSides.D6 to 1, DieSides.D8 to 1, DieSides.D10 to 1)
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            ChooseEffectDieRequest(
                effect = GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW,
                legalChoices = listOf(
                    EffectDieChoice(0, 4, 1),
                    EffectDieChoice(1, 6, 6)
                ),
                context = context
            )
        )

        assertEquals(1, chosen.index)
    }

    @Test
    fun `Overgrowth equivalent resulting size uses expected Battle value`() {
        val context = context(
            rows = listOf(
                row(StrikeRow.TOP, player(p1, 4, die(0, 4, 4)), player(p2, 8)),
                row(StrikeRow.MIDDLE, player(p1, 1, die(1, 6, 1)), player(p2, 8))
            ),
            graftBed = mapOf(DieSides.D8 to 1, DieSides.D12 to 1)
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            ChooseEffectDieRequest(
                effect = GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW,
                legalChoices = listOf(EffectDieChoice(0, 4, 4), EffectDieChoice(1, 6, 1)),
                context = context
            )
        )

        assertEquals(1, chosen.index)
    }

    @Test
    fun `exact special-effect target tie delegates to StrategyRandomizer`() {
        val randomizer = RecordingRandomizer(1)
        val strategy = HumanBaselineEffectStrategy(
            scoreEngine = BaselineScoreEngine(randomizer)
        )
        val context = context(
            rows = listOf(
                row(StrikeRow.TOP, player(p1, 10), player(p2, 5)),
                row(StrikeRow.MIDDLE, player(p1, 10), player(p2, 5))
            ),
            opponentWisps = 3
        )

        val chosen = strategy.chooseStrikeRow(
            ChooseEffectStrikeRowRequest(
                effect = GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW,
                legalChoices = listOf(StrikeRow.TOP, StrikeRow.MIDDLE),
                context = context
            )
        )

        assertEquals(StrikeRow.MIDDLE, chosen)
        assertEquals(listOf(2), randomizer.bounds)
    }

    private fun context(
        rows: List<BattleRowView>,
        opponentWisps: Int = 0,
        graftBed: Map<DieSides, Int> = emptyMap()
    ): DecisionContext {
        val hand = rows.flatMap { it.forPlayer(p1)?.dice.orEmpty() }
            .associateBy { it.handIndex }
            .values.sortedBy { it.handIndex }
            .map { DieView(it.handIndex, it.sides, it.value) }
        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(id = p1, hand = hand)
            ),
            opponents = listOf(
                OpponentView(
                    board = DecisionContext.EMPTY.self.board.copy(id = p2),
                    wispCount = opponentWisps
                )
            ),
            grove = DecisionContext.EMPTY.grove.copy(graftBed = graftBed),
            battle = BattleView(
                playerOrder = listOf(p1, p2),
                rows = rows
            )
        )
    }

    private fun row(row: StrikeRow, vararg players: BattlePlayerRowView) =
        BattleRowView(row, false, players.map { it.copy(row = row) })

    private fun player(id: PlayerId, total: Int, vararg dice: BattleDieView) =
        BattlePlayerRowView(
            playerId = id,
            row = StrikeRow.TOP,
            dice = dice.toList(),
            critters = emptyList(),
            dieTotal = total,
            critterTotal = 0,
            total = total,
            withdrawn = false
        )

    private fun die(index: Int, sides: Int, value: Int) = BattleDieView(index, sides, value)

    private class RecordingRandomizer(private val result: Int) : StrategyRandomizer {
        val bounds = mutableListOf<Int>()
        override fun nextInt(until: Int): Int {
            bounds += until
            return result
        }
    }
}
