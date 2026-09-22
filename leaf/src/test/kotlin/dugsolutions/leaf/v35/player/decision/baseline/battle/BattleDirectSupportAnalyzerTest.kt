package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.Token
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BattleDirectSupportAnalyzerTest {
    private val actor = PlayerId(0)
    private val opponent = PlayerId(1)
    private val analyzer = BattleDirectSupportAnalyzer()

    @Test
    fun `Bee uses current boosted value and shared deterministic analysis`() {
        val action = BattleSupportAction.PlaceCritter(Critter.BEE, StrikeRow.TOP)
        val result = requireNotNull(
            analyzer(
                context(actorTotal = 4, opponentTotal = 7, beeValue = 4),
                action
            )
        )

        assertEquals(BattleAnalysisMode.DETERMINISTIC, result.analysis.mode)
        assertEquals(4.0, result.analysis.swing.rowSwings.single().rawSwing)
        assertEquals(BattleTransition.WIN_FLIPPED, result.analysis.swing.rowSwings.single().transition)
        assertEquals(2, result.analysis.vpImpact.gain)
        assertEquals(BattleDirectSupportGate.NONE, result.gate)
        assertTrue(result.individuallyWorthwhile)
    }

    @Test
    fun `direct Worm passes when it projects the policy minimum Strike VP gain`() {
        val result = requireNotNull(
            analyzer(
                context(actorTotal = 4, opponentTotal = 5, wormValue = 2),
                BattleSupportAction.PlaceCritter(Critter.WORM, StrikeRow.TOP)
            )
        )

        assertEquals(BattleTransition.WIN_FLIPPED, result.analysis.swing.rowSwings.single().transition)
        assertEquals(BattleDirectSupportGate.NONE, result.gate)
        assertTrue(result.individuallyWorthwhile)
    }

    @Test
    fun `direct Worm fails when it does not project meaningful Strike VP gain`() {
        val result = requireNotNull(
            analyzer(
                context(actorTotal = 4, opponentTotal = 8, wormValue = 2),
                BattleSupportAction.PlaceCritter(Critter.WORM, StrikeRow.TOP)
            )
        )

        assertEquals(0, result.analysis.vpImpact.gain)
        assertEquals(
            BattleDirectSupportGate.WORM_REQUIRES_MEANINGFUL_VP_GAIN,
            result.gate
        )
        assertFalse(result.passesSpendingGate)
        assertFalse(result.individuallyWorthwhile)
    }

    @Test
    fun `Butterfly uses expected keep-better gain on its exact die row`() {
        val die = BattleDieView(handIndex = 3, sides = 6, value = 1)
        val result = requireNotNull(
            analyzer(
                context(actorTotal = 1, opponentTotal = 3, actorDice = listOf(die)),
                BattleSupportAction.Shared(
                    SupportAction.UseButterfly(
                        butterfly = Butterfly.GREEN,
                        die = HandDieChoice(index = 3, sides = 6, value = 1)
                    )
                )
            )
        )

        assertEquals(BattleAnalysisMode.EXPECTED, result.analysis.mode)
        assertEquals(2.5, result.analysis.swing.rowSwings.single().rawSwing)
        assertEquals(BattleTransition.WIN_FLIPPED, result.analysis.swing.rowSwings.single().transition)
        assertTrue(result.individuallyWorthwhile)
    }

    @Test
    fun `Water reroll passes only when expectation crosses a positive named transition`() {
        val die = BattleDieView(handIndex = 0, sides = 6, value = 1)
        val action = BattleSupportAction.Shared(
            SupportAction.UseWaterReroll(
                HandDieChoice(index = 0, sides = 6, value = 1)
            )
        )

        val flipsWin = requireNotNull(
            analyzer(
                context(actorTotal = 1, opponentTotal = 3, actorDice = listOf(die)),
                action
            )
        )
        val onlyPadsLead = requireNotNull(
            analyzer(
                context(actorTotal = 11, opponentTotal = 5, actorDice = listOf(die)),
                action
            )
        )

        assertEquals(BattleDirectSupportGate.PASSED, flipsWin.gate)
        assertTrue(flipsWin.individuallyWorthwhile)
        assertEquals(
            BattleDirectSupportGate.WATER_REQUIRES_POSITIVE_TRANSITION,
            onlyPadsLead.gate
        )
        assertFalse(onlyPadsLead.passesSpendingGate)
        assertFalse(onlyPadsLead.individuallyWorthwhile)
    }

    @Test
    fun `Water reroll may project harm without consuming RNG`() {
        val die = BattleDieView(handIndex = 2, sides = 6, value = 6)
        val result = requireNotNull(
            analyzer(
                context(actorTotal = 6, opponentTotal = 5, actorDice = listOf(die)),
                BattleSupportAction.Shared(
                    SupportAction.UseWaterReroll(
                        HandDieChoice(index = 2, sides = 6, value = 6)
                    )
                )
            )
        )

        assertEquals(BattleAnalysisMode.EXPECTED, result.analysis.mode)
        assertTrue(result.analysis.tacticalValue < 0.0)
        assertFalse(result.individuallyWorthwhile)
    }

    @Test
    fun `Mulch uses expected roll and best legal placement when it flips a win`() {
        val action = BattleSupportAction.Shared(
            SupportAction.UseMulch(Token.MULCH(DieSides.D6))
        )
        val result = requireNotNull(
            analyzer(
                multiRowContext(
                    row(StrikeRow.TOP, actorTotal = 4, opponentTotal = 7),
                    row(StrikeRow.MIDDLE, actorTotal = 12, opponentTotal = 1)
                ),
                action
            )
        )

        assertEquals(BattleAnalysisMode.EXPECTED, result.analysis.mode)
        assertEquals(StrikeRow.TOP, result.analysis.swing.rowSwings.single().row)
        assertEquals(BattleTransition.WIN_FLIPPED, result.analysis.swing.rowSwings.single().transition)
        assertEquals(BattleDirectSupportGate.PASSED, result.gate)
        assertTrue(result.individuallyWorthwhile)
    }

    @Test
    fun `Mulch fails its premium gate when expected best placement does not flip a win`() {
        val result = requireNotNull(
            analyzer(
                context(actorTotal = 1, opponentTotal = 10),
                BattleSupportAction.Shared(
                    SupportAction.UseMulch(Token.MULCH(DieSides.D4))
                )
            )
        )

        assertEquals(BattleDirectSupportGate.MULCH_REQUIRES_WIN_FLIPPED, result.gate)
        assertFalse(result.passesSpendingGate)
        assertFalse(result.individuallyWorthwhile)
    }

    @Test
    fun `enabling and target-dependent Wisp Supports are not given invented realizations`() {
        val wisp = WispCard(
            quantity = 1,
            name = "Wisp_Upgrade_Die",
            title = "Overgrowth",
            count = 1,
            effect = GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW,
            lineIcons = null,
            lineIconsHeight = 40,
            vpIcon = null,
            mainBackdrop = ""
        )

        assertNull(
            analyzer(
                context(actorTotal = 1, opponentTotal = 3),
                BattleSupportAction.Shared(SupportAction.PlayWisp(wisp))
            )
        )
        assertNull(
            analyzer(
                context(actorTotal = 1, opponentTotal = 3),
                BattleSupportAction.Shared(SupportAction.UseWaterRefresh)
            )
        )
    }

    private fun context(
        actorTotal: Int,
        opponentTotal: Int,
        beeValue: Int = 2,
        wormValue: Int = 1,
        actorDice: List<BattleDieView> = emptyList()
    ): DecisionContext =
        multiRowContext(
            row(
                StrikeRow.TOP,
                actorTotal,
                opponentTotal,
                actorDice
            ),
            beeValue = beeValue,
            wormValue = wormValue
        )

    private fun multiRowContext(
        vararg rows: BattleRowView,
        beeValue: Int = 2,
        wormValue: Int = 1
    ): DecisionContext =
        DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    id = actor,
                    beeValue = beeValue,
                    wormValue = wormValue
                )
            ),
            battle = BattleView(
                playerOrder = listOf(actor, opponent),
                rows = rows.toList()
            )
        )

    private fun row(
        strikeRow: StrikeRow,
        actorTotal: Int,
        opponentTotal: Int,
        actorDice: List<BattleDieView> = emptyList()
    ): BattleRowView =
        BattleRowView(
            row = strikeRow,
            closed = false,
            players = listOf(
                BattlePlayerRowView(
                    playerId = actor,
                    row = strikeRow,
                    dice = actorDice,
                    critters = emptyList(),
                    dieTotal = actorTotal,
                    critterTotal = 0,
                    total = actorTotal,
                    withdrawn = false
                ),
                BattlePlayerRowView(
                    playerId = opponent,
                    row = strikeRow,
                    dice = emptyList(),
                    critters = emptyList(),
                    dieTotal = opponentTotal,
                    critterTotal = 0,
                    total = opponentTotal,
                    withdrawn = false
                )
            )
        )
}
