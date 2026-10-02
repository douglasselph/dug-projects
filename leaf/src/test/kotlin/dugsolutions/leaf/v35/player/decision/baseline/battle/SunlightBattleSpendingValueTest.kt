package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
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

        assertEquals(45, observed.fundedMainValue)
        assertEquals(62, observed.preservationValue)
        assertEquals(-17, observed.netSupportValue)
        assertFalse(observed.worthwhile)
    }

    @Test
    fun `clearly strong extra action is worth one Sunlight even in early Battle`() {
        val observed = SunlightBattleSpendingValue.observe(
            context = context(sunlight = 1, battlesRemaining = 3),
            roundCard = strongRound(),
            action = BattleMainAction.RoundEffect1
        )

        assertEquals(70, observed.fundedMainValue)
        assertEquals(62, observed.preservationValue)
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

        assertEquals(60, early.fundedMainValue)
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

        assertEquals(52, withTwo.preservationValue)
        assertTrue(withTwo.worthwhile)
        assertEquals(62, afterFirstSpend.preservationValue)
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
    fun `high value Draw can justify Sunlight before final Battle`() {
        val observed = SunlightBattleSpendingValue.observe(
            context = context(
                sunlight = 1,
                battlesRemaining = 3,
                supplyDieSides = 20
            ),
            roundCard = weakRound(),
            action = BattleMainAction.Draw
        )

        assertEquals(87, observed.fundedMainValue)
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
        assertEquals("60", observations["fundedMainValue"])
        assertEquals("45", observations["sunlightPreservationValue"])
        assertEquals("15", observations["sunlightNetSupportValue"])
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
                isFinalBattleRound = finalBattle
            ),
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    sunlight = sunlight,
                    supply = supplyDieSides?.let { listOf(DieView(0, it, 1)) } ?: emptyList()
                )
            )
        )

    private fun weakRound() = round(GameEffect.RAISE_DIE_PLUS_3)
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
