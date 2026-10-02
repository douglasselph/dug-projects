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
    fun `no future Battle makes a new Sunlight worthless`() {
        val context = context(battlesRemaining = 0, upcoming = emptyList())

        val observation = SunlightTokenPriority.observe(context)

        assertEquals(0, observation.finalPriority)
        assertEquals(0, SunlightTokenPriority.score(context).total)
    }

    @Test
    fun `Battle next with three strong face-up Battle Plants values extra capacity`() {
        val context = context(
            battlesRemaining = 1,
            upcoming = listOf(RoundCardType.BATTLE),
            creature = listOf(
                plant(1, "Root_09_01"),
                plant(2, "Root_09_02"),
                plant(3, "Flower_11_03")
            )
        )

        val observation = SunlightTokenPriority.observe(context)

        assertEquals(75, observation.thirdBestFaceUpBattlePlantBase)
        assertTrue(observation.visibleBattlePlantAdjustment > 0)
        assertTrue(observation.finalPriority >= 60)
    }

    @Test
    fun `several stored Sunlights sharply reduce value with one Battle left`() {
        val empty = context(
            battlesRemaining = 1,
            upcoming = listOf(RoundCardType.BATTLE),
            sunlight = 0,
            creature = strongBattlePlants()
        )
        val stocked = context(
            battlesRemaining = 1,
            upcoming = listOf(RoundCardType.BATTLE),
            sunlight = 3,
            creature = strongBattlePlants()
        )

        val emptyScore = SunlightTokenPriority.score(empty).total
        val stockedObservation = SunlightTokenPriority.observe(stocked)

        assertTrue(emptyScore - stockedObservation.finalPriority >= 40)
        assertTrue(stockedObservation.finalPriority < 30)
    }

    @Test
    fun `distant Battle without obvious extra Plant action has only modest prospective value`() {
        val context = context(
            battlesRemaining = 1,
            upcoming = listOf(RoundCardType.CULTIVATION, RoundCardType.CULTIVATION, RoundCardType.BATTLE)
        )

        val observation = SunlightTokenPriority.observe(context)

        assertEquals(0, observation.battleProximityAdjustment)
        assertEquals(0, observation.visibleBattlePlantAdjustment)
        assertEquals(46, observation.finalPriority)
    }

    @Test
    fun `weak Draw can lose to Sunlight when Battle is next and extra Plant capacity is visible`() {
        val context = context(
            battlesRemaining = 1,
            upcoming = listOf(RoundCardType.BATTLE),
            supply = listOf(DieView(index = 0, sides = 4, value = 2)),
            creature = strongBattlePlants()
        )

        assertEquals(45, DrawPriority.score(context).total)
        assertTrue(SunlightTokenPriority.score(context).total > DrawPriority.score(context).total)
    }

    @Test
    fun `excellent immediate Main Action can outrank generic Sunlight without visible third-action pressure`() {
        val context = context(
            battlesRemaining = 1,
            upcoming = listOf(RoundCardType.BATTLE)
        )

        val sunlight = SunlightTokenPriority.score(context).total

        // 54 is deliberately well below the 70-80 range already used by the
        // Human Baseline for excellent current Plant actions.  The ordinary
        // Cultivation chooser performs the actual cross-action comparison.
        assertEquals(54, sunlight)
        assertTrue(sunlight < 70)
    }

    @Test
    fun `one remaining Battle already covered by stored Sunlight normally discourages another`() {
        val context = context(
            battlesRemaining = 1,
            upcoming = listOf(RoundCardType.BATTLE),
            sunlight = 1
        )

        val observation = SunlightTokenPriority.observe(context)

        assertEquals(-20, observation.existingSunlightAdjustment)
        assertEquals(34, observation.finalPriority)
    }

    @Test
    fun `three Battles and no stored Sunlight retain useful general option value`() {
        val context = context(
            battlesRemaining = 3,
            upcoming = listOf(
                RoundCardType.CULTIVATION,
                RoundCardType.BATTLE,
                RoundCardType.CULTIVATION,
                RoundCardType.BATTLE,
                RoundCardType.CULTIVATION,
                RoundCardType.BATTLE
            )
        )

        val observation = SunlightTokenPriority.observe(context)

        assertEquals(12, observation.battlesRemainingAdjustment)
        assertEquals(4, observation.battleProximityAdjustment)
        assertEquals(58, observation.finalPriority)
        assertEquals(observation.finalPriority, SunlightTokenPriority.score(context).total)
    }

    private fun strongBattlePlants(): List<CreatureCardView> = listOf(
        plant(1, "Root_09_01"),
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
            "Root_09_01" -> GameEffect.UPGRADE_DIE_AND_USE_NOW
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
