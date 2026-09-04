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
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.decision.effect.ChooseOEdelweissRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectStrikeRowRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectPlantChoice
import dugsolutions.leaf.v35.player.decision.effect.OEdelweissChoice
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OEdelweissEffectTest {

    @Test
    fun firstChoiceCanFlipFaceUpCard_thenSecondChoiceCanPlayThatNowSpentCard() {
        val strategy =
            FlipThenPlayStrategy()

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

        val target =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "RaiseVine",
                            PlantType.VINE,
                            GameEffect.RAISE_DIE_PLUS_4
                        )
                )
        val source =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "OEdelweiss",
                            PlantType.FLOWER,
                            GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE
                        )
                )

        val currentTarget =
            RecursivePlantEffectTestFixture
                .faceUp(
                    actor,
                    target
                )
        val currentSource =
            RecursivePlantEffectTestFixture
                .faceUp(
                    actor,
                    source
                )

        strategy.target =
            currentTarget.id
        strategy.source =
            currentSource.id

        DefaultGameEffectExecutor()
            .execute(
                EffectTestFixture.request(
                    game = game,
                    actor = actor,
                    effect =
                        GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
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
                .get(target.id)!!
                .isFaceDown
        )

        assertEquals(
            2,
            strategy.requests.size
        )

        val first =
            strategy.requests[0]
        assertTrue(
            first.any {
                it is OEdelweissChoice.Flip &&
                    it.card.cardId ==
                        target.id
            }
        )
        assertFalse(
            first.any {
                it is OEdelweissChoice.Play &&
                    it.card.cardId ==
                        target.id
            }
        )

        val second =
            strategy.requests[1]
        assertTrue(
            second.any {
                it is OEdelweissChoice.Play &&
                    it.card.cardId ==
                        target.id
            }
        )

        assertFalse(
            strategy.requests
                .flatten()
                .any { choice ->
                    when (choice) {
                        is OEdelweissChoice.Play ->
                            choice.card.cardId ==
                                source.id

                        is OEdelweissChoice.Flip ->
                            choice.card.cardId ==
                                source.id

                        OEdelweissChoice.Done ->
                            false
                    }
                }
        )
    }

    @Test
    fun replayedBattleEffect_receivesBattleStateAndCanManipulateChosenStrikeRow() {
        val strategy =
            PlayBattleEffectThenDoneStrategy(
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

        val target =
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
        val source =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "OEdelweiss",
                            PlantType.FLOWER,
                            GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE
                        )
                )
        val currentSource =
            RecursivePlantEffectTestFixture
                .faceUp(
                    actor,
                    source
                )

        strategy.target = target.id

        DefaultGameEffectExecutor()
            .execute(
                EffectTestFixture.request(
                    game = game,
                    actor = actor,
                    effect =
                        GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
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
                .get(target.id)!!
                .isFaceDown
        )
        assertEquals(
            2,
            strategy.calls
        )
    }

    @Test
    fun strategyCannotPlayFaceUpPlant() {
        val strategy =
            IllegalPlayFaceUpStrategy()

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

        val target =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "FreshVine",
                            PlantType.VINE,
                            GameEffect.RAISE_DIE_PLUS_4
                        )
                )
        val source =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "OEdelweiss",
                            PlantType.FLOWER,
                            GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE
                        )
                )

        val currentTarget =
            RecursivePlantEffectTestFixture
                .faceUp(
                    actor,
                    target
                )
        val currentSource =
            RecursivePlantEffectTestFixture
                .faceUp(
                    actor,
                    source
                )

        strategy.illegal =
            OEdelweissChoice.Play(
                EffectPlantChoice(
                    cardId =
                        currentTarget.id,
                    cardName =
                        currentTarget.card.name,
                    isFaceUp = true
                )
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
                            GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
                        source =
                            GameEffectSource.Plant(
                                currentSource
                            )
                    )
                )
        }

        assertTrue(
            actor.creature
                .get(target.id)!!
                .isFaceUp
        )
    }

    @Test
    fun doneEndsOptionalChoicesWithoutMutation() {
        val strategy =
            DoneStrategy()

        val actor =
            EffectTestFixture.player(
                id = 1,
                effectStrategy =
                    strategy
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        val target =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "TargetVine",
                            PlantType.VINE,
                            GameEffect.RAISE_DIE_PLUS_4
                        )
                )
        val source =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "OEdelweiss",
                            PlantType.FLOWER,
                            GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE
                        )
                )

        val currentSource =
            RecursivePlantEffectTestFixture
                .faceUp(
                    actor,
                    source
                )

        DefaultGameEffectExecutor()
            .execute(
                EffectTestFixture.request(
                    game = game,
                    actor = actor,
                    effect =
                        GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
                    source =
                        GameEffectSource.Plant(
                            currentSource
                        )
                )
            )

        assertEquals(
            1,
            strategy.calls
        )
        assertTrue(
            actor.creature
                .get(target.id)!!
                .isFaceDown
        )
    }

    @Test
    fun noOtherPlantMeansEffectCannotExecute() {
        val actor =
            EffectTestFixture.player(1)
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        // O Edelweiss requires a Vine anchor, so graft a Vine and then remove
        // it by clearing/rebuilding is not practical. Instead model the source
        // as an active-path card and make the only real graft excluded.
        val only =
            RecursivePlantEffectTestFixture
                .graft(
                    actor,
                    RecursivePlantEffectTestFixture
                        .plant(
                            "OnlyVine",
                            PlantType.VINE,
                            GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE
                        )
                )

        val request =
            EffectTestFixture.request(
                game = game,
                actor = actor,
                effect =
                    GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
                source =
                    GameEffectSource.Plant(
                        only
                    )
            )

        assertFalse(
            OEdelweissEffect()
                .canExecute(
                    request
                )
        )
    }

    private class FlipThenPlayStrategy :
        FirstEffectChoiceStrategy() {
        var target:
            CreatureCardId? = null
        var source:
            CreatureCardId? = null
        val requests =
            mutableListOf<
                List<OEdelweissChoice>
            >()

        override fun chooseOEdelweiss(
            request:
            ChooseOEdelweissRequest
        ): OEdelweissChoice {
            requests +=
                request.legalChoices

            return when (
                request.choiceNumber
            ) {
                1 ->
                    request.legalChoices
                        .filterIsInstance<
                            OEdelweissChoice.Flip
                        >()
                        .first {
                            it.card.cardId ==
                                target
                        }

                else ->
                    request.legalChoices
                        .filterIsInstance<
                            OEdelweissChoice.Play
                        >()
                        .first {
                            it.card.cardId ==
                                target
                        }
            }
        }
    }

    private class PlayBattleEffectThenDoneStrategy(
        private val row: StrikeRow
    ) : FirstEffectChoiceStrategy() {
        var target: CreatureCardId? = null
        var calls = 0

        override fun chooseOEdelweiss(
            request: ChooseOEdelweissRequest
        ): OEdelweissChoice {
            calls++
            if (request.choiceNumber == 2) {
                return OEdelweissChoice.Done
            }

            return request.legalChoices
                .filterIsInstance<OEdelweissChoice.Play>()
                .first {
                    it.card.cardId == target
                }
        }

        override fun chooseStrikeRow(
            request: ChooseEffectStrikeRowRequest
        ): StrikeRow = row
    }

    private class IllegalPlayFaceUpStrategy :
        FirstEffectChoiceStrategy() {
        lateinit var illegal:
            OEdelweissChoice

        override fun chooseOEdelweiss(
            request:
            ChooseOEdelweissRequest
        ): OEdelweissChoice =
            illegal
    }

    private class DoneStrategy :
        FirstEffectChoiceStrategy() {
        var calls = 0

        override fun chooseOEdelweiss(
            request:
            ChooseOEdelweissRequest
        ): OEdelweissChoice {
            calls++
            return OEdelweissChoice.Done
        }
    }
}
