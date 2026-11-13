package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import dugsolutions.leaf.v35.player.decision.baseline.card.CardScoringHelpers
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoredChoice
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SuppressedPlantAlternativeTraceTest {
    private val rootCard = PlantCard(
        6, "Root_05_02", "Root Four More", PlantType.ROOT, 5, null,
        "", "", "", "", "", "", "", GameEffect.RAISE_DIE_PLUS_4, PlantScoringRule.Fixed(1)
    )
    private val root = CreatureCard(
        CreatureCardId(42), rootCard, CreatureSide.LEFT, CreaturePosition(-1, 0),
        CreatureCard.Facing.FACE_UP
    )
    private val draw = CultivationAction.Main(CultivationMainAction.Draw)
    private val activate = CultivationAction.Main(CultivationMainAction.ActivatePlant(root))
    private val round = RoundCard(
        1, "test", RoundCardType.CULTIVATION,
        RoundCardEffect("first", "", "", "", null, GameEffect.GAIN_ONE_VP),
        RoundCardEffect("second", "", "", "", null, GameEffect.GAIN_ONE_VP), ""
    )

    private fun request(effect: GameEffect) = ChooseCultivationActionRequest(
        round, 2, listOf(draw, activate), context(effect)
    )

    private fun context(effect: GameEffect): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.CULTIVATION,
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(
                supply = listOf(DieView(0, 8, 1)),
                hand = listOf(DieView(0, 12, 2)),
                creature = listOf(CreatureCardView(
                    root.id, rootCard.name, rootCard.title, rootCard.type, rootCard.cost, effect,
                    rootCard.scoringRule, root.side, root.position, root.facing, true
                ))
            )
        )
    )

    @Test
    fun `Root Four More score exceeds D8 draw even before Raise value is counted`() {
        val view = context(GameEffect.RAISE_DIE_PLUS_4)
        val drawScore = DrawPriority.score(view)
        val baseOnly = CardScoringHelpers.playScore(view, CardPhase.CULTIVATION,
            GameEffect.RAISE_DIE_PLUS_4, rootCard.name, base = 58)
        assertEquals(53, drawScore.total)
        assertEquals(70, baseOnly.total)
        assertTrue(58 > drawScore.total) // Base priority alone already wins.
    }

    @Test
    fun `disabling the effect makes the policy choose Draw instead`() {
        val policy = HumanBaselineCultivationStrategy()
        val official = assertIs<CultivationAction.Main>(policy.chooseAction(request(GameEffect.RAISE_DIE_PLUS_4)))
        val suppressed = assertIs<CultivationAction.Main>(policy.chooseAction(request(GameEffect.RESEARCH_NO_EFFECT)))
        assertIs<CultivationMainAction.ActivatePlant>(official.action)
        assertEquals(CultivationMainAction.Draw, suppressed.action)
    }

    @Test
    fun `trace names actual substitute with competing scores and projected pips`() {
        val req = request(GameEffect.RESEARCH_NO_EFFECT)
        val options = listOf(
            ScoredChoice<CultivationAction>(draw, PriorityScore(53)),
            ScoredChoice<CultivationAction>(activate, PriorityScore(-10_000))
        )
        val row = assertNotNull(SuppressedPlantAlternativeTrace.describe(req, options, options[0]))
        assertTrue(row.startsWith("TRACE_SUPPRESSED_MAIN\t"))
        assertTrue("selected=DRAW" in row)
        assertTrue("suppressedScore=-10000" in row)
        assertTrue("drawScore=53" in row)
        assertTrue("nextDie=D8" in row)
        assertTrue("expectedDrawPips=4.5" in row)
        assertTrue("possibleOfficialRaisePips=4" in row)
        assertTrue("allMainScores=DRAW:53|PLANT:Root_05_02:-10000" in row)
    }

    @Test
    fun `trace ignores unsupported states and interim Support decisions`() {
        val official = request(GameEffect.RAISE_DIE_PLUS_4)
        val options = listOf(ScoredChoice<CultivationAction>(draw, PriorityScore(53)), ScoredChoice<CultivationAction>(activate, PriorityScore(70)))
        assertNull(SuppressedPlantAlternativeTrace.describe(official, options, options[0]))
        // A Main Action was not yet spent; this decision is not a replacement.
        val support = CultivationAction.Support(
            dugsolutions.leaf.v35.player.decision.support.SupportAction.UseWaterRefresh
        )
        val suppressed = request(GameEffect.RESEARCH_NO_EFFECT)
        val withSupport = options + ScoredChoice<CultivationAction>(support, PriorityScore(99))
        assertNull(SuppressedPlantAlternativeTrace.describe(suppressed, withSupport, withSupport.last()))
    }
}
