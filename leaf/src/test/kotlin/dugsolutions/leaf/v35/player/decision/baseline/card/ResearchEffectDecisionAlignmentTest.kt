package dugsolutions.leaf.v35.player.decision.baseline.card

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.SimpleEffectMagnitude
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.HumanBaselineCultivationStrategy
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleMainPriority
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.context.PlantStackView
import dugsolutions.leaf.v35.player.decision.baseline.projection.ExpectedDrawProjection
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Calibration controls must affect action valuation, not just engine execution. */
class ResearchEffectDecisionAlignmentTest {
    private val registry = HumanBaselineCardScorerRegistry()

    private fun context(name: String, effect: GameEffect, hand: List<DieView>, supply: List<DieView> = emptyList()) =
        DecisionContext.EMPTY.copy(
            phase = RoundCardType.CULTIVATION,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(hand = hand, supply = supply)
            ),
            grove = DecisionContext.EMPTY.grove.copy(
                plantStacks = listOf(PlantStackView(
                    name, name,
                    if (name.startsWith("Root")) PlantType.ROOT else PlantType.FLOWER,
                    if (name.startsWith("Root")) 5 else 17,
                    effect, PlantScoringRule.Fixed(1), 4
                ))
            )
        )

    private fun score(name: String, effect: GameEffect, hand: List<DieView>, supply: List<DieView> = emptyList()) =
        registry.forName(name).playScore(context(name, effect, hand, supply), CardPhase.CULTIVATION, name).total

    @Test fun `Root Four More reacts to stronger and disabled effective effect`() {
        val hand = listOf(DieView(0, 20, 3))
        val normal = score("Root_05_02", GameEffect.RAISE_DIE_PLUS_4, hand)
        val strong = score("Root_05_02", GameEffect.RESEARCH_RAISE_DIE_PLUS_20, hand)
        val disabled = score("Root_05_02", GameEffect.RESEARCH_NO_EFFECT, hand)
        assertTrue(strong > normal, "$strong should exceed $normal")
        assertTrue(disabled < 0, "An inert effect should not retain the printed Plant's activation priority")
    }

    @Test fun `Queens Blossom values actual four draw opportunity and no effect`() {
        val supply = (0..3).map { DieView(it, 8, 2) }
        val normal = score("Flower_17_04", GameEffect.DRAW_TWO_DICE, emptyList(), supply)
        val strong = score("Flower_17_04", GameEffect.RESEARCH_DRAW_FOUR_DICE, emptyList(), supply)
        assertTrue(strong > normal, "$strong should exceed $normal")
        assertTrue(score("Flower_17_04", GameEffect.RESEARCH_NO_EFFECT, emptyList(), supply) < 0)
        // No fictitious four new dice when only two are actually available.
        val restricted = context("Flower_17_04", GameEffect.RESEARCH_DRAW_FOUR_DICE,
            emptyList(), supply.take(2))
        assertEquals(2, ExpectedDrawProjection.forecast(restricted, 4).sides.size)
    }

    @Test fun `plus twenty VP is prioritized for both cards without changing printed VP`() {
        for (name in listOf("Root_05_02", "Flower_17_04")) {
            val hand = listOf(DieView(0, 4, 4))
            val score = score(name, GameEffect.RESEARCH_GAIN_TWENTY_VP, hand)
            assertTrue(score > 500, "$name must recognize twenty immediate VP: $score")
            assertEquals(20, SimpleEffectMagnitude.immediateVp(GameEffect.RESEARCH_GAIN_TWENTY_VP))
        }
    }

    @Test fun `experimental Raise targets realizable twenty sided gain`() {
        val context = context("Root_05_02", GameEffect.RESEARCH_RAISE_DIE_PLUS_20,
            listOf(DieView(0, 8, 1), DieView(1, 20, 6)))
        val small = CardScoringHelpers.scoreDieTarget(
            GameEffect.RESEARCH_RAISE_DIE_PLUS_20, context, EffectDieChoice(0, 8, 1)
        ).total
        val large = CardScoringHelpers.scoreDieTarget(
            GameEffect.RESEARCH_RAISE_DIE_PLUS_20, context, EffectDieChoice(1, 20, 6)
        ).total
        assertTrue(large > small, "Raise +20 should choose +14 over +7, not use the old +4 ranking")
    }

    @Test fun `Cultivation action selection follows seat effective effects rather than printed effect`() {
        val printed = PlantCard(6, "Root_05_02", "Root Four More", PlantType.ROOT, 5,
            null, "", "", "", "", "", "", "", GameEffect.RAISE_DIE_PLUS_4, PlantScoringRule.Fixed(1))
        val card = CreatureCard(CreatureCardId(42), printed, CreatureSide.LEFT,
            CreaturePosition(-1, 0), CreatureCard.Facing.FACE_UP)
        val legal = listOf(
            CultivationAction.Main(CultivationMainAction.Draw),
            CultivationAction.Main(CultivationMainAction.ActivatePlant(card))
        )
        fun choose(effectiveEffect: GameEffect): CultivationMainAction {
            val original = context("Root_05_02", effectiveEffect,
                listOf(DieView(0, 20, 19)), listOf(DieView(1, 6, 2)))
            val view = CreatureCardView(card.id, printed.name, printed.title,
                printed.type, printed.cost, effectiveEffect, printed.scoringRule,
                card.side, card.position, card.facing, true)
            val ctx = original.copy(self = original.self.copy(
                board = original.self.board.copy(creature = listOf(view))))
            val choice = HumanBaselineCultivationStrategy().chooseAction(
                ChooseCultivationActionRequest(roundCard(), 2, legal, ctx)
            ) as CultivationAction.Main
            return choice.action
        }
        assertEquals(CultivationMainAction.Draw, choose(GameEffect.RESEARCH_NO_EFFECT))
        assertTrue(choose(GameEffect.RESEARCH_GAIN_TWENTY_VP) is CultivationMainAction.ActivatePlant)
    }

    @Test fun `Battle Main priority also reads experimental effect from player view`() {
        val printed = PlantCard(6, "Root_05_02", "Root Four More", PlantType.ROOT, 5,
            null, "", "", "", "", "", "", "", GameEffect.RAISE_DIE_PLUS_4, PlantScoringRule.Fixed(1))
        val card = CreatureCard(CreatureCardId(7), printed, CreatureSide.LEFT,
            CreaturePosition(-1, 0), CreatureCard.Facing.FACE_UP)
        fun priority(effect: GameEffect): Int {
            val view = CreatureCardView(card.id, printed.name, printed.title,
                printed.type, printed.cost, effect, printed.scoringRule,
                card.side, card.position, card.facing, true)
            val base = context("Root_05_02", effect, listOf(DieView(0, 20, 19)))
            val battle = base.copy(phase = RoundCardType.BATTLE,
                self = base.self.copy(board = base.self.board.copy(creature = listOf(view))))
            return BattleMainPriority.intrinsicScore(battle, roundCard(),
                BattleMainAction.ActivatePlant(card)).total
        }
        assertTrue(priority(GameEffect.RESEARCH_NO_EFFECT) < 0)
        assertTrue(priority(GameEffect.RESEARCH_GAIN_TWENTY_VP) > 500)
    }

    private fun roundCard() = RoundCard(
        quantity = 1, name = "calibration", type = RoundCardType.CULTIVATION,
        firstEffect = RoundCardEffect("first", "", "", "", null, GameEffect.GAIN_ONE_VP),
        secondEffect = RoundCardEffect("second", "", "", "", null, GameEffect.GAIN_ONE_VP),
        backImage = ""
    )

}
