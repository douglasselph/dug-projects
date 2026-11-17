package dugsolutions.leaf.v35.player.decision.baseline

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ResourcePersonalityTest {
    @Test
    fun `four named personalities have independent and legible resource tendencies`() {
        val neutral = ResourcePersonality.NEUTRAL
        val keeper = ResourcePersonality.WATER_KEEPER
        val preparer = ResourcePersonality.BATTLE_PREPARER
        val specialist = ResourcePersonality.WATER_MULCH_NO_SUNLIGHT

        assertEquals(4, ResourcePersonality.NAMED.map { it.id }.toSet().size)
        for (resource in PersonalityResource.values()) {
            assertEquals(ResourceInclination.NEUTRAL, neutral.goal(resource).inclination)
        }
        assertEquals(ResourceGoal(ResourceInclination.LIKE, 2), keeper.water)
        assertEquals(neutral.sunlight, keeper.sunlight)
        assertEquals(neutral.mulch, keeper.mulch)
        assertEquals(ResourceGoal(ResourceInclination.LIKE, 2), preparer.sunlight)
        assertEquals(ResourceGoal(ResourceInclination.LIKE, 2), preparer.mulch)
        assertEquals(ResourceInclination.NEUTRAL, preparer.water.inclination)
        assertEquals(ResourceGoal(ResourceInclination.LIKE, 2), specialist.water)
        assertEquals(ResourceGoal(ResourceInclination.AVOID, 0), specialist.sunlight)
        assertEquals(ResourceGoal(ResourceInclination.LIKE, 2), specialist.mulch)
    }

    @Test
    fun `custom personality can change one resource without changing others`() {
        val neutral = ResourcePersonality.NEUTRAL
        val customized = neutral.copy(
            id = "water-avoider",
            water = ResourceGoal(ResourceInclination.AVOID, 0)
        )
        assertEquals(ResourceInclination.AVOID, customized.water.inclination)
        assertEquals(neutral.sunlight, customized.sunlight)
        assertEquals(neutral.mulch, customized.mulch)
        assertNotEquals(neutral.description(), customized.description())
        assertEquals(ResourceInclination.NEUTRAL, ResourcePersonality.NEUTRAL.water.inclination)
    }

    @Test
    fun `invalid targets and nonreproducible ids are rejected`() {
        assertFailsWith<IllegalArgumentException> { ResourceGoal(softTarget = -1) }
        assertFailsWith<IllegalArgumentException> { ResourcePersonality(id = "") }
        assertFailsWith<IllegalArgumentException> { ResourcePersonality(id = "Water Keeper") }
    }

    @Test
    fun `descriptions record every preference and target without learned weights`() {
        assertEquals(
            "profile=water-mulch-no-sunlight;water=LIKE:2;sunlight=AVOID:0;mulch=LIKE:2",
            ResourcePersonality.WATER_MULCH_NO_SUNLIGHT.description()
        )
    }
}
