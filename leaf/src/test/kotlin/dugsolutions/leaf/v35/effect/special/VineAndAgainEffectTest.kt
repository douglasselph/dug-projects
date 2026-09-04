package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.error.InvalidDecisionException
import dugsolutions.leaf.v35.effect.DefaultGameEffectExecutor
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FirstEffectChoiceStrategy
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectPlantRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectStrikeRowRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectPlantChoice
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VineAndAgainEffectTest {

    @Test
    fun choosesExactSpentRootOrVine_andRecursivelyExecutesItsEffect() {
        val strategy =
            ChoosePlantEffectStrategy()

        val die =
            FixedEffectDie(
                sides = 8,
                value = 1
            )
        val actor =
            EffectTestFixture.player(
                id = 1,
                hand = listOf(die),
                effectStrategy =
                    strategy
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        val source =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "VineAgain",
                            PlantType.VINE,
                            GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT
                        )
                )
        val spentRoot =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "SpentRoot",
                            PlantType.ROOT,
                            GameEffect.RAISE_DIE_PLUS_4
                        )
                )
        val faceUpRoot =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "FreshRoot",
                            PlantType.ROOT,
                            GameEffect.RAISE_DIE_PLUS_4
                        )
                )
        RecursivePlantEffectTestFixture
            .faceUp(
                actor,
                faceUpRoot
            )

        val currentSource =
            RecursivePlantEffectTestFixture
                .faceUp(
                    actor,
                    source
                )

        strategy.target =
            spentRoot.id

        DefaultGameEffectExecutor()
            .execute(
                EffectTestFixture.request(
                    game = game,
                    actor = actor,
                    effect =
                        GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT,
                    source =
                        GameEffectSource.Plant(
                            currentSource
                        )
                )
            )

        assertEquals(
            5,
            die.value
        )
        assertTrue(
            actor.creature
                .get(spentRoot.id)!!
                .isFaceDown
        )
        assertEquals(
            listOf(spentRoot.id),
            strategy.offered.map {
                it.cardId
            }
        )
    }

    @Test
    fun nestedBattleEffect_receivesBattleStateAndCanManipulateChosenStrikeRow() {
        val strategy =
            ChoosePlantAndRowEffectStrategy(
                StrikeRow.MIDDLE
            )

        val actorDie =
            FixedEffectDie(
                sides = 8,
                value = 4
            )
        val actor =
            EffectTestFixture.player(
                id = 1,
                hand = listOf(actorDie),
                effectStrategy = strategy
            )
        val opponentDie =
            FixedEffectDie(
                sides = 10,
                value = 8
            )
        val opponent =
            EffectTestFixture.player(
                id = 2,
                hand = listOf(opponentDie)
            )
        val game =
            EffectTestFixture.game(
                actor,
                opponent
            )
        val battleState =
            BattleState(
                listOf(actor, opponent)
            )

        battleState.grid.placeDie(
            actor,
            StrikeRow.MIDDLE,
            actorDie
        )
        battleState.grid.placeDie(
            opponent,
            StrikeRow.MIDDLE,
            opponentDie
        )

        val source =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "VineAgain",
                            PlantType.VINE,
                            GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT
                        )
                )
        val spentVine =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "VinePunishment",
                            PlantType.VINE,
                            GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3
                        )
                )
        val currentSource =
            RecursivePlantEffectTestFixture
                .faceUp(
                    actor,
                    source
                )

        strategy.target = spentVine.id

        DefaultGameEffectExecutor()
            .execute(
                EffectTestFixture.request(
                    game = game,
                    actor = actor,
                    effect =
                        GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT,
                    source =
                        GameEffectSource.Plant(
                            currentSource
                        )
                ).copy(
                    phase = GameEffectPhase.BATTLE,
                    battleState = battleState
                )
            )

        assertEquals(
            5,
            opponentDie.value
        )
        assertEquals(
            StrikeRow.MIDDLE,
            battleState.grid.locationOf(
                opponentDie
            )?.row
        )
        assertTrue(
            actor.creature
                .get(spentVine.id)!!
                .isFaceDown
        )
    }

    @Test
    fun faceUpRootOrVine_isNotALegalReuseTarget() {
        val strategy =
            IllegalPlantEffectStrategy()

        val actor =
            EffectTestFixture.player(
                id = 1,
                hand =
                    listOf(
                        FixedEffectDie(
                            8,
                            1
                        )
                    ),
                effectStrategy =
                    strategy
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        val source =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "VineAgain",
                            PlantType.VINE,
                            GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT
                        )
                )
        val spent =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "SpentRoot",
                            PlantType.ROOT,
                            GameEffect.RAISE_DIE_PLUS_4
                        )
                )
        val fresh =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "FreshRoot",
                            PlantType.ROOT,
                            GameEffect.RAISE_DIE_PLUS_4
                        )
                )

        val currentSource =
            RecursivePlantEffectTestFixture
                .faceUp(
                    actor,
                    source
                )
        val currentFresh =
            RecursivePlantEffectTestFixture
                .faceUp(
                    actor,
                    fresh
                )

        strategy.choice =
            EffectPlantChoice(
                cardId =
                    currentFresh.id,
                cardName =
                    currentFresh.card.name,
                isFaceUp = true
            )

        assertFailsWith<
            InvalidDecisionException
        > {
            DefaultGameEffectExecutor()
                .execute(
                    EffectTestFixture.request(
                        game = game,
                        actor = actor,
                        effect =
                            GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT,
                        source =
                            GameEffectSource.Plant(
                                currentSource
                            )
                    )
                )
        }

        assertTrue(
            actor.creature
                .get(fresh.id)!!
                .isFaceUp
        )
        assertTrue(
            actor.creature
                .get(spent.id)!!
                .isFaceDown
        )
    }

    @Test
    fun flowersAreNeverReuseTargets() {
        val actor =
            EffectTestFixture.player(
                id = 1,
                hand =
                    listOf(
                        FixedEffectDie(
                            8,
                            1
                        )
                    )
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        val source =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "VineAgain",
                            PlantType.VINE,
                            GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT
                        )
                )
        // A Flower needs a Vine anchor. The source Vine supplies it.
        RecursivePlantEffectTestFixture
            .graft(
                actor,
                RecursivePlantEffectTestFixture
                    .plant(
                        "SpentFlower",
                        PlantType.FLOWER,
                        GameEffect.RAISE_DIE_PLUS_4
                    )
            )

        val currentSource =
            RecursivePlantEffectTestFixture
                .faceUp(
                    actor,
                    source
                )

        val request =
            EffectTestFixture.request(
                game = game,
                actor = actor,
                effect =
                    GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT,
                source =
                    GameEffectSource.Plant(
                        currentSource
                    )
            )

        assertFalse(
            VineAndAgainEffect()
                .canExecute(
                    request
                )
        )
    }

    @Test
    fun recursionPathExcludesAlreadyActivePlant() {
        val actor =
            EffectTestFixture.player(
                id = 1
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        val source =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "VineAgain",
                            PlantType.VINE,
                            GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT
                        )
                )
        val target =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "OtherAgain",
                            PlantType.ROOT,
                            GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT
                        )
                )

        val currentSource =
            RecursivePlantEffectTestFixture
                .faceUp(
                    actor,
                    source
                )

        val request =
            EffectTestFixture.request(
                game = game,
                actor = actor,
                effect =
                    GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT,
                source =
                    GameEffectSource.Plant(
                        currentSource
                    )
            ).copy(
                plantEffectPath =
                    listOf(
                        target.id
                    )
            )

        assertFalse(
            VineAndAgainEffect()
                .canExecute(
                    request
                )
        )
    }

    private open class ChoosePlantEffectStrategy :
        FirstEffectChoiceStrategy() {
        var target:
            CreatureCardId? = null
        var offered:
            List<EffectPlantChoice> =
            emptyList()

        override fun choosePlantEffect(
            request:
            ChooseEffectPlantRequest
        ): EffectPlantChoice {
            offered =
                request.legalChoices

            return request
                .legalChoices
                .first {
                    target == null ||
                        it.cardId ==
                            target
                }
        }
    }

    private class ChoosePlantAndRowEffectStrategy(
        private val row: StrikeRow
    ) : ChoosePlantEffectStrategy() {
        override fun chooseStrikeRow(
            request: ChooseEffectStrikeRowRequest
        ): StrikeRow = row
    }

    private class IllegalPlantEffectStrategy :
        ChoosePlantEffectStrategy() {
        lateinit var choice:
            EffectPlantChoice

        override fun choosePlantEffect(
            request:
            ChooseEffectPlantRequest
        ): EffectPlantChoice {
            offered =
                request.legalChoices
            return choice
        }
    }
}
