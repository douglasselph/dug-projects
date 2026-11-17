package dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource

import dugsolutions.leaf.v35.player.decision.baseline.ResourceGoal
import dugsolutions.leaf.v35.player.decision.baseline.ResourceInclination
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ResourceAcquisitionPreferenceTest {
    private val score = PriorityScore(50).adjusted(2, "Useful existing Round opportunity")

    @Test
    fun `neutral leaves original score and explanation unchanged`() {
        assertEquals(score, ResourceAcquisitionPreference.apply(
            score, "Water", ResourceGoal(), currentlyHeld = 0
        ))
    }

    @Test
    fun `like encourages a missing first or second unit without making a hard quota`() {
        val goal = ResourceGoal(ResourceInclination.LIKE, softTarget = 2)
        val first = ResourceAcquisitionPreference.apply(score, "Water", goal, 0)
        val second = ResourceAcquisitionPreference.apply(score, "Water", goal, 1)
        val stocked = ResourceAcquisitionPreference.apply(score, "Water", goal, 2)
        assertTrue(first.total > second.total)
        assertTrue(second.total > score.total)
        assertTrue(stocked.total < score.total)
        assertTrue(first.explanation().lines.any { it.reason.contains("soft aim 2") })
    }

    @Test
    fun `avoid discourages rather than makes the action illegal`() {
        val avoided = ResourceAcquisitionPreference.apply(
            score, "Sunlight", ResourceGoal(ResourceInclination.AVOID, 0), 0
        )
        assertEquals(score.total - 12, avoided.total)
        assertTrue(avoided.total > 0)
    }

    @Test
    fun `an unavailable future use is never rescued by liking its resource`() {
        val goal = ResourceGoal(ResourceInclination.LIKE, 2)
        assertEquals(PriorityScore(0), ResourceAcquisitionPreference.apply(
            PriorityScore(0), "Sunlight", goal, 0
        ))
        assertEquals(score, ResourceAcquisitionPreference.apply(
            score, "Mulch", goal, 0, viable = false
        ))
    }

    @Test
    fun `negative held counts fail fast and large aims have bounded influence`() {
        val goal = ResourceGoal(ResourceInclination.LIKE, 30)
        assertEquals(12, ResourceAcquisitionPreference.apply(score, "Water", goal, 0).total - score.total)
        assertFailsWith<IllegalArgumentException> {
            ResourceAcquisitionPreference.apply(score, "Water", goal, -1)
        }
    }
}
