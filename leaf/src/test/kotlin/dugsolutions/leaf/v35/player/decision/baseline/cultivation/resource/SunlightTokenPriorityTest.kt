package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.DrawPriority
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SunlightTokenPriorityTest {
    @Test
    fun `no remaining Battle makes new Sunlight worthless`() {
        val state = context(battlesRemaining = 0, upcoming = emptyList())
        assertEquals(0, SunlightTokenPriority.score(state).total)
        assertEquals(0.0, SunlightTokenPriority.observe(state).estimatedPipValue)
    }

    @Test
    fun `token valuation is calibrated against Draw rather than arbitrary Plant bases`() {
        val state = context(battlesRemaining = 1, upcoming = listOf(RoundCardType.BATTLE))
        assertEquals(4.5, SunlightTokenPriority.observe(state).estimatedPipValue)
        assertEquals(53, SunlightTokenPriority.score(state).total)
    }

    @Test
    fun `existing tokens lower the marginal acquisition value`() {
        val initial = context(battlesRemaining = 2, upcoming = listOf(RoundCardType.BATTLE))
        val stocked = context(battlesRemaining = 2, upcoming = listOf(RoundCardType.BATTLE), sunlight = 1)
        assertTrue(SunlightTokenPriority.score(initial).total > SunlightTokenPriority.score(stocked).total)
    }

    @Test
    fun `surplus token remains low priority`() {
        val state = context(battlesRemaining = 1, upcoming = listOf(RoundCardType.BATTLE), sunlight = 2)
        assertEquals(1.0, SunlightTokenPriority.observe(state).estimatedPipValue)
        assertEquals(39, SunlightTokenPriority.score(state).total)
    }

    @Test
    fun `visible Battle Plant base scores do not change Sunlight acquisition value`() {
        val empty = context(battlesRemaining = 1, upcoming = listOf(RoundCardType.BATTLE))
        val planted = context(battlesRemaining = 1, upcoming = listOf(RoundCardType.BATTLE), creature = strongBattlePlants())
        assertEquals(SunlightTokenPriority.score(empty).total, SunlightTokenPriority.score(planted).total)
    }

    @Test
    fun `Sunlight competes with Draw by ordinary score comparison`() {
        val state = context(
            battlesRemaining = 1,
            upcoming = listOf(RoundCardType.BATTLE),
            supply = listOf(DieView(index = 0, sides = 4, value = 2))
        )
        assertEquals(45, DrawPriority.score(state).total)
        assertTrue(SunlightTokenPriority.score(state).total > DrawPriority.score(state).total)
    }

    private fun strongBattlePlants(): List<CreatureCardView> = listOf(
        plant(1, "Root_07_01"),
        plant(2, "Root_09_02"),
        plant(3, "Flower_11_03")
    )

    private fun context(
        battlesRemaining: Int,
        upcoming: List<RoundCardType>,
        sunlight: Int = 0,
        supply: List<DieView> = emptyList(),
        creature: List<CreatureCardView> = emptyList()
    ): DecisionContext {
        val base = DecisionContext.EMPTY
        return base.copy(
            phase = RoundCardType.CULTIVATION,
            progress = base.progress.copy(
                battleRoundsRemaining = battlesRemaining,
                upcomingRoundTypes = upcoming
            ),
            self = base.self.copy(
                board = base.self.board.copy(
                    sunlight = sunlight,
                    supply = supply,
                    creature = creature
                )
            )
        )
    }

    private fun plant(id: Int, name: String): CreatureCardView = CreatureCardView(
        id = CreatureCardId(id),
        name = name,
        title = name,
        type = if (name.startsWith("Flower")) PlantType.FLOWER else PlantType.ROOT,
        cost = if (name.startsWith("Flower")) 11 else 9,
        effect = when (name) {
            "Root_07_01" -> GameEffect.UPGRADE_DIE_AND_USE_NOW
            "Root_09_02" -> GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE
            "Flower_11_03" -> GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW
            else -> GameEffect.RAISE_ANY_DIE_PLUS_1
        },
        scoringRule = PlantScoringRule.Fixed(1),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-id, 0),
        facing = CreatureCard.Facing.FACE_UP,
        isSnippable = true
    )
}
