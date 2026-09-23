package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleEnabledPlantAnalyzerTest {
    private val analyzer = BattleEnabledPlantAnalyzer()

    @Test
    fun `max-die Plant projects its best immediate deterministic Battle use`() {
        val opportunity = analyzer(
            context(actorTotal = 4, opponentTotal = 10, dieSides = 12, dieValue = 4),
            plant(
                name = "Vine_11_04",
                effect = GameEffect.SET_DIE_UP_TO_D12_TO_MAX,
                type = PlantType.VINE
            )
        )

        val tactical = requireNotNull(opportunity.tacticalAnalysis)
        assertEquals(BattleAnalysisMode.DETERMINISTIC, tactical.mode)
        assertEquals(BattleTransition.WIN_FLIPPED, tactical.swing.rowSwings.single().transition)
        assertEquals(3, tactical.improvementStepCount)
        assertTrue(opportunity.priority.total > 500)
    }

    @Test
    fun `sapping snapdragon top-level value includes opposing row drain`() {
        val opportunity = analyzer(
            context(
                actorTotal = 8,
                opponentTotal = 10,
                dieSides = 6,
                dieValue = 5,
                opponentDice = listOf(
                    BattleDieView(handIndex = 1, sides = 6, value = 6),
                    BattleDieView(handIndex = 2, sides = 6, value = 4)
                )
            ),
            plant(
                name = "Flower_17_03",
                effect = GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW,
                type = PlantType.FLOWER
            )
        )

        val tactical = requireNotNull(opportunity.tacticalAnalysis)
        assertEquals(BattleTransition.WIN_FLIPPED, tactical.swing.rowSwings.single().transition)
        assertTrue(tactical.tacticalValue >= 500.0)
    }

    @Test
    fun `reroll Plant stays expected and does not execute hypothetical RNG`() {
        val opportunity = analyzer(
            context(actorTotal = 1, opponentTotal = 3, dieSides = 6, dieValue = 1),
            plant(
                name = "Root_05_03",
                effect = GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS,
                type = PlantType.ROOT
            )
        )

        val tactical = requireNotNull(opportunity.tacticalAnalysis)
        assertEquals(BattleAnalysisMode.EXPECTED, tactical.mode)
        assertEquals(3.5, tactical.swing.rowSwings.single().rawSwing)
    }

    private fun context(
        actorTotal: Int,
        opponentTotal: Int,
        dieSides: Int,
        dieValue: Int,
        opponentDice: List<BattleDieView> = emptyList()
    ): DecisionContext {
        val die = BattleDieView(handIndex = 0, sides = dieSides, value = dieValue)
        return DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    id = ACTOR,
                    hand = listOf(DieView(index = 0, sides = dieSides, value = dieValue))
                )
            ),
            battle = BattleView(
                playerOrder = listOf(ACTOR, OPPONENT),
                rows = listOf(
                    BattleRowView(
                        row = StrikeRow.TOP,
                        closed = false,
                        players = listOf(
                            BattlePlayerRowView(
                                ACTOR,
                                StrikeRow.TOP,
                                listOf(die),
                                emptyList(),
                                actorTotal,
                                0,
                                actorTotal,
                                false
                            ),
                            BattlePlayerRowView(
                                OPPONENT,
                                StrikeRow.TOP,
                                opponentDice,
                                emptyList(),
                                opponentTotal,
                                0,
                                opponentTotal,
                                false
                            )
                        )
                    )
                )
            )
        )
    }

    private fun plant(
        name: String,
        effect: GameEffect,
        type: PlantType
    ): CreatureCardView = CreatureCardView(
        id = CreatureCardId(1),
        name = name,
        title = name,
        type = type,
        cost = 5,
        effect = effect,
        scoringRule = PlantScoringRule.Fixed(1),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-1, 0),
        facing = CreatureCard.Facing.FACE_DOWN,
        isSnippable = true
    )

    private companion object {
        val ACTOR = PlayerId(0)
        val OPPONENT = PlayerId(1)
    }
}
