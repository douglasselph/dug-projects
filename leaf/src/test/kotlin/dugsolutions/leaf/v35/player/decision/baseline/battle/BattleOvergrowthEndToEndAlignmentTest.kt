package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.effect.HumanBaselineEffectStrategy
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.context.WispView
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleOvergrowthEndToEndAlignmentTest {
    private val actor = PlayerId(0)
    private val opponent = PlayerId(1)

    @Test
    fun `top-level rejects Overgrowth when B13 larger-result target is tactically harmful`() {
        val d4 = die(0, 4, 1)
        val d6 = die(1, 6, 6)
        val context = context(
            rows = listOf(
                row(StrikeRow.TOP, player(actor, 1, d4), player(opponent, 2)),
                row(StrikeRow.MIDDLE, player(actor, 6, d6), player(opponent, 6))
            ),
            graftBed = mapOf(
                DieSides.D6 to 1,
                DieSides.D8 to 1,
                DieSides.D10 to 1
            )
        )

        // The D4 -> D8 realization would flip TOP into a win, but B13 correctly
        // prioritizes the larger legal D6 -> D10 result. That chosen realization
        // is expected to break the MIDDLE tie against us, so top-level willingness
        // must not value Overgrowth from the tactically nicer D4 alternative.
        val support = support()
        val priority = BattleSupportPriority().score(context, support)
        val orchestration = BattleTurnOrchestrator()(
            context = context,
            roundCard = round(),
            legalChoices = listOf(
                BattleTurnAction.Support(support),
                BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1)
            )
        )
        val downstream = chooseTarget(
            context,
            listOf(
                EffectDieChoice(0, 4, 1),
                EffectDieChoice(1, 6, 6)
            )
        )

        assertTrue(priority.total < 0)
        assertFalse(orchestration.chooseSupport)
        assertEquals(1, downstream.index)
    }

    @Test
    fun `top-level plays Overgrowth using the same expected tie-break that selects its B13 target`() {
        val d4 = die(0, 4, 4)
        val d6 = die(1, 6, 1)
        val context = context(
            rows = listOf(
                row(StrikeRow.TOP, player(actor, 4, d4), player(opponent, 8)),
                row(StrikeRow.MIDDLE, player(actor, 1, d6), player(opponent, 8))
            ),
            graftBed = mapOf(
                DieSides.D8 to 1,
                DieSides.D12 to 1
            )
        )

        // Both targets reach D12. B13 therefore uses expected current-Battle
        // value as its tie-break and prefers the D6 on the wound-exposed row.
        // Top-level willingness must see that same expected realization.
        val support = support()
        val orchestration = BattleTurnOrchestrator()(
            context = context,
            roundCard = round(),
            legalChoices = listOf(
                BattleTurnAction.Support(support),
                BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1)
            )
        )
        val downstream = chooseTarget(
            context,
            listOf(
                EffectDieChoice(0, 4, 4),
                EffectDieChoice(1, 6, 1)
            )
        )

        assertTrue(BattleSupportPriority().score(context, support).total > 0)
        assertTrue(orchestration.chooseSupport)
        assertEquals(1, downstream.index)
    }

    private fun chooseTarget(
        context: DecisionContext,
        legalChoices: List<EffectDieChoice>
    ): EffectDieChoice =
        HumanBaselineEffectStrategy().chooseDie(
            ChooseEffectDieRequest(
                effect = GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW,
                legalChoices = legalChoices,
                context = context
            )
        )

    private fun support(): BattleSupportAction =
        BattleSupportAction.Shared(SupportAction.PlayWisp(overgrowth()))

    private fun overgrowth() = WispCard(
        quantity = 4,
        name = "Wisp_Upgrade_Die",
        title = "Overgrowth",
        count = 4,
        effect = GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW,
        lineIcons = null,
        lineIconsHeight = 40,
        vpIcon = null,
        mainBackdrop = "",
        endGameVp = 0
    )

    private fun context(
        rows: List<BattleRowView>,
        graftBed: Map<DieSides, Int>
    ): DecisionContext {
        val hand = rows.flatMap { it.forPlayer(actor)?.dice.orEmpty() }
            .associateBy { it.handIndex }
            .values
            .sortedBy { it.handIndex }
            .map { DieView(it.handIndex, it.sides, it.value) }

        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    id = actor,
                    hand = hand
                ),
                wisps = listOf(
                    WispView(
                        index = 0,
                        name = "Wisp_Upgrade_Die",
                        title = "Overgrowth",
                        effect = GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW,
                        playImmediately = false,
                        battleOnly = false,
                        endGameVp = 0
                    )
                )
            ),
            grove = DecisionContext.EMPTY.grove.copy(graftBed = graftBed),
            battle = BattleView(
                playerOrder = listOf(actor, opponent),
                rows = rows
            )
        )
    }

    private fun row(row: StrikeRow, vararg players: BattlePlayerRowView) =
        BattleRowView(row, false, players.map { it.copy(row = row) })

    private fun player(
        id: PlayerId,
        total: Int,
        vararg dice: BattleDieView
    ) = BattlePlayerRowView(
        playerId = id,
        row = StrikeRow.TOP,
        dice = dice.toList(),
        critters = emptyList(),
        dieTotal = total,
        critterTotal = 0,
        total = total,
        withdrawn = false
    )

    private fun die(index: Int, sides: Int, value: Int) =
        BattleDieView(index, sides, value)

    private fun round() = RoundCard(
        quantity = 1,
        name = "Battle_Test",
        type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("Bloom", "", "", "", null, GameEffect.GAIN_ONE_VP),
        secondEffect = RoundCardEffect("Burrow", "", "", "", null, GameEffect.GAIN_TWO_WORMS),
        backImage = ""
    )
}
