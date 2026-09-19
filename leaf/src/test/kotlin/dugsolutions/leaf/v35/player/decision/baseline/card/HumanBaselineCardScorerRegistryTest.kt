package dugsolutions.leaf.v35.player.decision.baseline.card

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineCardScorerRegistryTest {
    @Test
    fun `every current Plant and Wisp CSV key has a scorer`() {
        val expected = setOf(
            "Root_05_01", "Root_05_02", "Root_05_03", "Root_05_04",
            "Root_07_01", "Root_07_02", "Root_07_03", "Root_07_04",
            "Root_09_01", "Root_09_02", "Root_09_03", "Root_09_04",
            "Vine_07_01", "Vine_07_02", "Vine_07_03", "Vine_07_04",
            "Vine_09_01", "Vine_09_02", "Vine_09_03", "Vine_09_04",
            "Vine_11_01", "Vine_11_02", "Vine_11_03", "Vine_11_04",
            "Flower_11_01", "Flower_11_02", "Flower_11_03", "Flower_11_04",
            "Flower_14_01", "Flower_14_02", "Flower_14_03", "Flower_14_04",
            "Flower_17_01", "Flower_17_02", "Flower_17_03", "Flower_17_04",
            "Wisp_Award_VP", "Wisp_Award_VP2", "Wisp_Gain_Critters",
            "Wisp_Gain_Green", "Wisp_Gain_Purple", "Wisp_Gain_Red", "Wisp_Gain_Yellow",
            "Wisp_Mulch_Die", "Wisp_Quake", "Wisp_Swap_Die",
            "Wisp_Upgrade_Die", "Wisps_Resolve"
        )
        assertEquals(expected, HumanBaselineCardScorerRegistry().registeredNames())
    }
}
