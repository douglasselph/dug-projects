package dugsolutions.leaf.integration.v35.sanity.effect

import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectDecisionRequirements
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * CSV-to-engine contract audit.
 *
 * This deliberately does not execute every effect in an enormous bespoke game
 * state. Unit tests own individual mechanics. The integration contract instead
 * proves that every real card definition parses to a known GameEffect, has a
 * production dispatcher route, and has an explicit decision-surface contract
 * for every phase in which that card can present the effect.
 */
class CardEffectContractTest {

    @Test
    fun `every loaded Plant Wisp and Round effect parses routes and declares decisions`() {
        IntegrationGameHarness().use { harness ->
            val occurrences = buildList {
                harness.catalog.allPlants.forEach { card ->
                    add(
                        EffectOccurrence(
                            source = "Plant ${card.name}",
                            effect = card.effect,
                            phases = setOf(
                                GameEffectPhase.CULTIVATION,
                                GameEffectPhase.BATTLE
                            )
                        )
                    )
                }

                harness.catalog.allWisps.forEach { card ->
                    add(
                        EffectOccurrence(
                            source = "Wisp ${card.name}",
                            effect = card.effect,
                            phases = if (card.battleOnly) {
                                setOf(GameEffectPhase.BATTLE)
                            } else {
                                setOf(
                                    GameEffectPhase.CULTIVATION,
                                    GameEffectPhase.BATTLE
                                )
                            }
                        )
                    )
                }

                harness.catalog.allRounds.forEach { card ->
                    val phase = when (card.type) {
                        RoundCardType.CULTIVATION -> GameEffectPhase.CULTIVATION
                        RoundCardType.BATTLE -> GameEffectPhase.BATTLE
                    }
                    add(
                        EffectOccurrence(
                            source = "Round ${card.name}/FIRST",
                            effect = card.firstEffect.effect,
                            phases = setOf(phase)
                        )
                    )
                    add(
                        EffectOccurrence(
                            source = "Round ${card.name}/SECOND",
                            effect = card.secondEffect.effect,
                            phases = setOf(phase)
                        )
                    )
                }
            }

            // 36 Plant definitions + 12 Wisp definitions + 12x2 Round effects.
            assertEquals(72, occurrences.size)

            occurrences.forEach { occurrence ->
                assertNotEquals(
                    GameEffect.UNKNOWN,
                    occurrence.effect,
                    "${occurrence.source} did not parse to a known GameEffect"
                )
                assertTrue(
                    harness.effectExecutor.supports(occurrence.effect),
                    "${occurrence.source} parsed as ${occurrence.effect} but has no production executor route"
                )

                val contract = GameEffectDecisionRequirements.forEffect(occurrence.effect)
                assertNotNull(
                    contract,
                    "${occurrence.source} parsed as ${occurrence.effect} but has no decision contract"
                )
                occurrence.phases.forEach { phase ->
                    // Calling forPhase is the contract assertion. Empty is a
                    // valid, explicit result for deterministic effects.
                    GameEffectDecisionRequirements.forPhase(
                        effect = occurrence.effect,
                        phase = phase
                    )
                }
            }
        }
    }

    @Test
    fun `official CSV effects are represented while research variants remain optional`() {
        IntegrationGameHarness().use { harness ->
            val loaded = buildSet {
                harness.catalog.allPlants.mapTo(this) { it.effect }
                harness.catalog.allWisps.mapTo(this) { it.effect }
                harness.catalog.allRounds.forEach { card ->
                    add(card.firstEffect.effect)
                    add(card.secondEffect.effect)
                }
            }

            // The GameEffect enum is intentionally a superset of the official
            // CSV catalog: it also contains retired mechanics and controlled
            // Petal/Sapping research variants. Do not require enum equality.
            val officialExamples = mapOf(
                // November 2026 official definitions. The deprecated alternatives
                // remain in GameEffect only for historical research/replay.
                "Root Awakening" to GameEffect.UPGRADE_DIE_CONDITIONAL_TWO_STEPS_AND_USE_NOW,
                "Root Appreciation" to GameEffect.GAIN_WORM_BOOST_AND_FREE_BATTLE_SUPPORT,
                "Root Down Payment" to GameEffect.GAIN_VP_PER_ONE_SHOWING,
                "Vine Yield" to GameEffect.RAISE_ONE_DIE_PLUS_1_PER_GRAFTED_FLOWER,
                "Saplink Trellis" to GameEffect.RAISE_TWO_DICE_PLUS_1_PER_ROOT_OR_VINE,
                "Berry Tasty" to GameEffect.RAISE_ALL_D8S_PLUS_2,
                "Rise and Vine" to GameEffect.RAISE_ALL_DICE_PLUS_2,
                "Vine and Punishment" to GameEffect.REROLL_DIE_ON_3_DRAW_ONE_CULTIVATION_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3,
                "Alluring Nectar" to GameEffect.GAIN_OR_STEAL_BUTTERFLY_REFRESH_AND_FREE_BATTLE_SUPPORT,
                "Bloom Backbone" to GameEffect.RAISE_ALL_DICE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER,
                "Someday Sunflower" to GameEffect.GAIN_TWO_SUNLIGHT_TOKENS,
                "Bee-loved Bloom" to GameEffect.GAIN_OR_STEAL_BEE_BOOST_AND_FREE_BATTLE_SUPPORT,
                "Bloom Backflip" to GameEffect.FLIP_OWN_DIE_AND_OPPONENT_DIE_IN_BATTLE,
                "Petal To Die 4" to GameEffect.GAIN_D4_USE_NOW_THEN_CYCLE_D4S,
                "Forget-Me-Not" to GameEffect.USE_DIE_FROM_DISCARD_NOW,
                "Sapping Snapdragon" to GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW,
                "O Edelweiss" to GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE
            )
            officialExamples.forEach { (title, expected) ->
                val card = harness.catalog.requirePlant(title)
                assertEquals(expected, card.effect, "Official effect for $title")
                assertTrue(expected in loaded, "$title is absent from the loaded effect catalog")
            }
            // Wisp and Battle Round titles/IDs are intentionally stable even
            // when their printed effect is revised. Guard those mappings too.
            val officialWisps = mapOf(
                "Wisp_Award_VP" to GameEffect.CHOOSE_PLAYER_WITH_WISPS_TRASH_ONE,
                "Wisp_Award_VP2" to GameEffect.EACH_PLAYER_WITH_WISPS_TRASH_ONE,
                "Wisps_Last_Word" to GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW
            )
            officialWisps.forEach { (id, expected) ->
                val card = harness.catalog.requireWisp(id)
                assertEquals(expected, card.effect, "Official effect for Wisp $id")
                assertTrue(expected in loaded, "$id is absent from the loaded effect catalog")
            }

            val officialBattleRoundSeconds = mapOf(
                "Battle_Bloom_Surge" to GameEffect.GAIN_UP_TO_D12_TO_DISCARD,
                "Battle_Scatter_Swell" to GameEffect.GAIN_UP_TO_D8_AND_USE_NOW
            )
            officialBattleRoundSeconds.forEach { (id, expected) ->
                val card = harness.catalog.requireRound(id)
                assertEquals(expected, card.secondEffect.effect, "Official second effect for Round $id")
                assertTrue(expected in loaded, "$id is absent from the loaded effect catalog")
            }

            assertTrue(GameEffect.UNKNOWN !in loaded)
            assertTrue(
                GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW !in loaded,
                "Sapping S2 should remain an opt-in research variant"
            )
        }
    }

    private data class EffectOccurrence(
        val source: String,
        val effect: GameEffect,
        val phases: Set<GameEffectPhase>
    )
}
