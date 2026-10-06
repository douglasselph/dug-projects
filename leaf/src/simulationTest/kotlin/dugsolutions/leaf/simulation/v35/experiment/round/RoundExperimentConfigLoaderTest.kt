package dugsolutions.leaf.simulation.v35.experiment.round

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.RoundEffectSlot
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RoundExperimentConfigLoaderTest {
    private val sunlightCompost = card("Resource_Sunlight_Compost", GameEffect.RAISE_DIE_PLUS_3, GameEffect.UPGRADE_DIE_FROM_HAND)
    private val sunlightWater = card("Resource_Sunlight_Water", GameEffect.RAISE_DIE_PLUS_3, GameEffect.GAIN_WATER_TOKEN)
    private val compostMulch = card("Resource_Compost_Mulch", GameEffect.UPGRADE_DIE_FROM_HAND, GameEffect.MULCH_DIE_FROM_HAND)
    private val waterCompost = card("Resource_Water_Compost", GameEffect.GAIN_WATER_TOKEN, GameEffect.UPGRADE_DIE_FROM_HAND)
    private val cards = listOf(sunlightCompost, sunlightWater, compostMulch, waterCompost)
    private val loader = RoundExperimentConfigLoader(cards)

    @Test
    fun emptyContent_preservesCanonicalEffects() {
        val config = loader.parse("")
        assertEquals(GameEffect.RAISE_DIE_PLUS_3, config.effectFor(sunlightCompost, RoundEffectSlot.FIRST))
        assertEquals(GameEffect.UPGRADE_DIE_FROM_HAND, config.effectFor(sunlightCompost, RoundEffectSlot.SECOND))
    }

    @Test
    fun oneOverride_replacesOnlySpecifiedSlot() {
        val config = loader.parse(
            """round_card,effect_slot,effect
Resource_Sunlight_Compost,1,GAIN_SUNLIGHT_TOKEN
"""
        )
        assertEquals(GameEffect.GAIN_SUNLIGHT_TOKEN, config.effectFor(sunlightCompost, RoundEffectSlot.FIRST))
        assertEquals(GameEffect.UPGRADE_DIE_FROM_HAND, config.effectFor(sunlightCompost, RoundEffectSlot.SECOND))
        assertEquals(GameEffect.RAISE_DIE_PLUS_3, config.effectFor(sunlightWater, RoundEffectSlot.FIRST))
    }

    @Test
    fun compostUseNow_canRerouteEveryCanonicalCompostSlot() {
        val config = loader.parse(
            """round_card,effect_slot,effect
Resource_Compost_Mulch,1,UPGRADE_DIE_AND_USE_NOW
Resource_Sunlight_Compost,2,UPGRADE_DIE_AND_USE_NOW
Resource_Water_Compost,2,UPGRADE_DIE_AND_USE_NOW
"""
        )

        assertEquals(GameEffect.UPGRADE_DIE_AND_USE_NOW, config.effectFor(compostMulch, RoundEffectSlot.FIRST))
        assertEquals(GameEffect.UPGRADE_DIE_AND_USE_NOW, config.effectFor(sunlightCompost, RoundEffectSlot.SECOND))
        assertEquals(GameEffect.UPGRADE_DIE_AND_USE_NOW, config.effectFor(waterCompost, RoundEffectSlot.SECOND))

        // Unspecified companion effects remain canonical.
        assertEquals(GameEffect.MULCH_DIE_FROM_HAND, config.effectFor(compostMulch, RoundEffectSlot.SECOND))
        assertEquals(GameEffect.RAISE_DIE_PLUS_3, config.effectFor(sunlightCompost, RoundEffectSlot.FIRST))
        assertEquals(GameEffect.GAIN_WATER_TOKEN, config.effectFor(waterCompost, RoundEffectSlot.FIRST))
    }

    @Test
    fun multipleOverrides_applyIndependently() {
        val config = loader.parse(
            """round_card,effect_slot,effect
Resource_Sunlight_Compost,1,GAIN_SUNLIGHT_TOKEN
Resource_Sunlight_Water,1,GAIN_SUNLIGHT_TOKEN
"""
        )
        assertEquals(GameEffect.GAIN_SUNLIGHT_TOKEN, config.effectFor(sunlightCompost, RoundEffectSlot.FIRST))
        assertEquals(GameEffect.GAIN_SUNLIGHT_TOKEN, config.effectFor(sunlightWater, RoundEffectSlot.FIRST))
    }

    @Test
    fun unknownRoundCard_isRejected() {
        val error = assertFailsWith<IllegalArgumentException> {
            loader.parse("round_card,effect_slot,effect\nNot_A_Card,1,GAIN_SUNLIGHT_TOKEN\n")
        }
        assert(error.message.orEmpty().contains("Unknown Round Card ID"))
    }

    @Test
    fun invalidEffectSlot_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            loader.parse("round_card,effect_slot,effect\nResource_Sunlight_Compost,3,GAIN_SUNLIGHT_TOKEN\n")
        }
    }

    @Test
    fun unknownGameEffect_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            loader.parse("round_card,effect_slot,effect\nResource_Sunlight_Compost,1,NOT_REAL\n")
        }
    }

    @Test
    fun duplicateConflictingSlot_isRejected() {
        val error = assertFailsWith<IllegalArgumentException> {
            loader.parse(
                """round_card,effect_slot,effect
Resource_Sunlight_Compost,1,GAIN_SUNLIGHT_TOKEN
Resource_Sunlight_Compost,1,GAIN_WATER_TOKEN
"""
            )
        }
        assert(error.message.orEmpty().contains("Duplicate conflicting"))
    }

    @Test
    fun load_readsHumanReadableCsvFromPath() {
        val path = Files.createTempFile("round-overrides", ".csv")
        try {
            Files.writeString(path, "round_card,effect_slot,effect\nResource_Sunlight_Compost,1,GAIN_SUNLIGHT_TOKEN\n")
            val config = loader.load(path.toString())
            assertEquals(GameEffect.GAIN_SUNLIGHT_TOKEN, config.effectFor(sunlightCompost, RoundEffectSlot.FIRST))
        } finally {
            Files.deleteIfExists(path)
        }
    }

    private fun card(name: String, first: GameEffect, second: GameEffect): RoundCard =
        RoundCard(
            quantity = 2,
            name = name,
            type = RoundCardType.CULTIVATION,
            firstEffect = effect(first),
            secondEffect = effect(second),
            backImage = "back"
        )

    private fun effect(effect: GameEffect): RoundCardEffect =
        RoundCardEffect("title", "bg", "fg", "image", null, effect)
}
