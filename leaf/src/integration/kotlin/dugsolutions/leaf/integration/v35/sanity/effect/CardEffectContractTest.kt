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

            // 36 Plant definitions + 13 Wisp definitions + 12x2 Round effects.
            assertEquals(73, occurrences.size)

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
    fun `current CSV catalog represents every defined non UNKNOWN GameEffect`() {
        IntegrationGameHarness().use { harness ->
            val loaded = buildSet {
                harness.catalog.allPlants.mapTo(this) { it.effect }
                harness.catalog.allWisps.mapTo(this) { it.effect }
                harness.catalog.allRounds.forEach { card ->
                    add(card.firstEffect.effect)
                    add(card.secondEffect.effect)
                }
            }
            val defined = GameEffect.entries
                .filterNot { it == GameEffect.UNKNOWN }
                .toSet()

            assertEquals(
                defined,
                loaded,
                "GameEffect enum and real CSV-backed effect catalog have drifted"
            )
            assertEquals(59, loaded.size)
        }
    }

    private data class EffectOccurrence(
        val source: String,
        val effect: GameEffect,
        val phases: Set<GameEffectPhase>
    )
}
