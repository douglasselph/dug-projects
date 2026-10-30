package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.GameProgressView
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SunlightBattleSpendingValueTest {
    @Test
    fun `early Battle preserves last Sunlight when only weak extra action exists`() {
        val observed = SunlightBattleSpendingValue.observe(
            context = context(sunlight = 1, battlesRemaining = 3),
            roundCard = weakRound(),
            action = BattleMainAction.RoundEffect1
        )

        assertEquals(35, observed.fundedMainValue)
        assertEquals(44, observed.preservationValue)
        assertEquals(-9, observed.netSupportValue)
        assertFalse(observed.worthwhile)
    }

    @Test
    fun `clearly strong extra action is worth one Sunlight even in early Battle`() {
        val observed = SunlightBattleSpendingValue.observe(
            context = context(sunlight = 1, battlesRemaining = 3),
            roundCard = strongRound(),
            action = BattleMainAction.RoundEffect1
        )

        assertEquals(56, observed.fundedMainValue)
        assertEquals(44, observed.preservationValue)
        assertTrue(observed.worthwhile)
    }

    @Test
    fun `useful but merely marginal action is preserved early and spent later`() {
        val early = SunlightBattleSpendingValue.observe(
            context = context(sunlight = 1, battlesRemaining = 3),
            roundCard = usefulRound(),
            action = BattleMainAction.RoundEffect1
        )
        val middle = SunlightBattleSpendingValue.observe(
            context = context(sunlight = 1, battlesRemaining = 2),
            roundCard = usefulRound(),
            action = BattleMainAction.RoundEffect1
        )

        assertEquals(43, early.fundedMainValue)
        assertFalse(early.worthwhile)
        assertTrue(middle.worthwhile)
    }

    @Test
    fun `surplus Sunlight makes first spend easier while preserving the last token`() {
        val withTwo = SunlightBattleSpendingValue.observe(
            context = context(sunlight = 2, battlesRemaining = 3),
            roundCard = usefulRound(),
            action = BattleMainAction.RoundEffect1
        )
        val afterFirstSpend = SunlightBattleSpendingValue.observe(
            context = context(sunlight = 1, battlesRemaining = 3),
            roundCard = usefulRound(),
            action = BattleMainAction.RoundEffect1
        )

        assertEquals(40, withTwo.preservationValue)
        assertTrue(withTwo.worthwhile)
        assertEquals(44, afterFirstSpend.preservationValue)
        assertFalse(afterFirstSpend.worthwhile)
    }

    @Test
    fun `final Battle sharply reduces preservation pressure`() {
        val observed = SunlightBattleSpendingValue.observe(
            context = context(sunlight = 1, battlesRemaining = 1, finalBattle = true),
            roundCard = weakRound(),
            action = BattleMainAction.RoundEffect1
        )

        assertEquals(15, observed.preservationValue)
        assertTrue(observed.worthwhile)
    }

    @Test
    fun `decision score equals observed net continuation value even with many tokens in final Battle`() {
        val context = context(sunlight = 8, battlesRemaining = 1, finalBattle = true)
        val observed = SunlightBattleSpendingValue.observe(
            context, weakRound(), BattleMainAction.RoundEffect1
        )
        val decision = SunlightBattleSpendingValue.score(
            context, weakRound(), BattleMainAction.RoundEffect1
        )

        // The raw base + final timing + surplus reductions fall below five.
        // Both the displayed diagnostic and real strategy must use the floor.
        assertEquals(5, observed.preservationValue)
        assertEquals(observed.netSupportValue, decision.total)
    }

    @Test
    fun `D20 Draw justifies Sunlight without a phantom final Main bonus`() {
        val observed = SunlightBattleSpendingValue.observe(
            context = context(
                sunlight = 1,
                battlesRemaining = 3,
                supplyDieSides = 20
            ),
            roundCard = weakRound(),
            action = BattleMainAction.Draw
        )

        // No Battle rows are supplied by this fixture, so there is no projected
        // Strike-row bonus: 35 Battle Draw origin + round(10.5) = 46.
        // The old 77 also used Cultivation x4; Battle uses one point per pip.
        assertEquals(46, observed.fundedMainValue)
        assertTrue(observed.worthwhile)
    }

    @Test
    fun `diagnostic observations expose funded Main preservation and net value`() {
        val observations = SunlightBattleSpendingValue.observations(
            context = context(sunlight = 2, battlesRemaining = 2),
            roundCard = usefulRound(),
            action = BattleMainAction.RoundEffect1
        )

        assertEquals("sunlight-battle-spend", observations["decisionFamily"])
        assertEquals("43", observations["fundedMainValue"])
        // Two held Sunlight tokens lower the marginal preservation value by four.
        // 39 base - 4 surplus = 35; funded action 43 - 35 = +8 net.
        assertEquals("35", observations["sunlightPreservationValue"])
        assertEquals("8", observations["sunlightNetSupportValue"])
    }

    private fun context(
        sunlight: Int,
        battlesRemaining: Int,
        finalBattle: Boolean = false,
        supplyDieSides: Int? = null
    ): DecisionContext =
        DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            progress = GameProgressView.EMPTY.copy(
                battleRoundsRemaining = battlesRemaining,
                totalBattleRounds = 3,
                isFinalBattleRound = finalBattle,
                // A die gained to Discard has value only if it can be drawn later.
                roundsRemainingToReveal = if (finalBattle) 0 else 2
            ),
            opponents = (1..3).map { id ->
                OpponentView(
                    DecisionContext.EMPTY.self.board.copy(id = PlayerId(id)),
                    wispCount = 1
                )
            },
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    sunlight = sunlight,
                    supply = supplyDieSides?.let { listOf(DieView(0, it, 1)) } ?: emptyList()
                )
            )
        )

    private fun weakRound() = round(GameEffect.RAISE_DIE_PLUS_4)
    private fun usefulRound() = round(GameEffect.GAIN_ANY_DIE_TO_DISCARD)
    private fun strongRound() = round(GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS)

    private fun round(effect: GameEffect) = RoundCard(
        quantity = 1,
        name = "Battle_Sunlight_Calibration",
        type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("first", "", "", "", null, effect),
        secondEffect = RoundCardEffect("second", "", "", "", null, GameEffect.GAIN_ONE_WISP),
        backImage = ""
    )
}
