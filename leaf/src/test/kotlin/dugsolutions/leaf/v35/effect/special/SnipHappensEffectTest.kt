package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.error.InvalidDecisionException
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FirstEffectChoiceStrategy
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectOpponentPlantWoundRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectOpponentPlantWoundChoice
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SnipHappensEffectTest {

    private val effect =
        SnipHappensEffect()

    private val nested =
        GameEffectExecutor { }

    @Test
    fun actorChoosesExactOpponentAndFaceUpPlantToWound() {
        val strategy =
            ChooseOpponentPlantStrategy()

        val actor =
            EffectTestFixture.player(
                id = 1,
                effectStrategy = strategy
            )
        val opponent2 =
            EffectTestFixture.player(2)
        val opponent3 =
            EffectTestFixture.player(3)

        val game =
            EffectTestFixture.game(
                actor,
                opponent2,
                opponent3
            )

        val card2 =
            graft(
                game,
                opponent2,
                PlantType.ROOT
            )
        val card3 =
            graft(
                game,
                opponent3,
                PlantType.VINE
            )

        opponent2.creature.faceUp(
            card2.id
        )
        opponent3.creature.faceUp(
            card3.id
        )

        strategy.targetOwner =
            opponent3.id
        strategy.targetCard =
            card3.id

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE
            ),
            nested
        )

        assertTrue(
            opponent2.creature
                .get(card2.id)!!
                .isFaceUp
        )
        assertTrue(
            opponent3.creature
                .get(card3.id)!!
                .isFaceDown
        )

        assertTrue(
            strategy.offered.any {
                it.ownerId == opponent2.id &&
                    it.cardId == card2.id
            }
        )
        assertTrue(
            strategy.offered.any {
                it.ownerId == opponent3.id &&
                    it.cardId == card3.id
            }
        )
    }

    @Test
    fun opponentWithAnyFaceUpPlantOffersOnlyFaceUpTargets() {
        val strategy =
            ChooseOpponentPlantStrategy()

        val actor =
            EffectTestFixture.player(
                id = 1,
                effectStrategy = strategy
            )
        val opponent =
            EffectTestFixture.player(2)
        val game =
            EffectTestFixture.game(
                actor,
                opponent
            )

        val inner =
            graftRoot(
                game,
                opponent
            )
        val outer =
            graftRootOutside(
                game,
                opponent,
                inner
            )

        opponent.creature.faceUp(
            inner.id
        )

        strategy.targetOwner =
            opponent.id
        strategy.targetCard =
            inner.id

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE
            ),
            nested
        )

        assertEquals(
            listOf(
                EffectOpponentPlantWoundChoice.Flip(
                    ownerId = opponent.id,
                    cardId = inner.id,
                    cardName = inner.card.name
                )
            ),
            strategy.offered
        )

        assertTrue(
            opponent.creature
                .get(inner.id)!!
                .isFaceDown
        )
        assertTrue(
            opponent.creature
                .get(outer.id)!!
                .isFaceDown
        )
    }

    @Test
    fun choosingFaceDownPlantWhileTargetOpponentHasFaceUpPlant_throwsBeforeMutation() {
        val actor =
            EffectTestFixture.player(
                id = 1,
                effectStrategy =
                    IllegalFaceDownTargetStrategy()
            )
        val opponent =
            EffectTestFixture.player(2)
        val game =
            EffectTestFixture.game(
                actor,
                opponent
            )

        val inner =
            graftRoot(
                game,
                opponent
            )
        val outer =
            graftRootOutside(
                game,
                opponent,
                inner
            )

        opponent.creature.faceUp(
            inner.id
        )

        val strategy =
            actor.decisions.effect
                as IllegalFaceDownTargetStrategy

        strategy.illegalChoice =
            EffectOpponentPlantWoundChoice.Snip(
                ownerId = opponent.id,
                cardId = outer.id,
                cardName = outer.card.name
            )

        assertFailsWith<
            InvalidDecisionException
        > {
            effect.execute(
                EffectTestFixture.request(
                    game,
                    actor,
                    GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE
                ),
                nested
            )
        }

        // Nothing was mutated.
        assertTrue(
            opponent.creature
                .get(inner.id)!!
                .isFaceUp
        )
        assertTrue(
            opponent.creature
                .get(outer.id)!!
                .isFaceDown
        )
        assertEquals(
            2,
            opponent.creature.size
        )
    }

    @Test
    fun allFaceDownOffersOnlyCurrentSnippableCards_andChosenCardReturnsToGrove() {
        val strategy =
            ChooseOpponentPlantStrategy()

        val actor =
            EffectTestFixture.player(
                id = 1,
                effectStrategy = strategy
            )
        val opponent =
            EffectTestFixture.player(2)
        val game =
            EffectTestFixture.game(
                actor,
                opponent
            )

        val inner =
            graftRoot(
                game,
                opponent
            )
        val outer =
            graftRootOutside(
                game,
                opponent,
                inner
            )

        val stack =
            game.grove.plantMarket
                .stackFor(
                    outer.card
                )!!

        val before =
            stack.remaining

        strategy.targetOwner =
            opponent.id
        strategy.targetCard =
            outer.id

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE
            ),
            nested
        )

        assertEquals(
            listOf(
                EffectOpponentPlantWoundChoice.Snip(
                    ownerId = opponent.id,
                    cardId = outer.id,
                    cardName = outer.card.name
                )
            ),
            strategy.offered
        )

        assertTrue(
            opponent.creature
                .get(inner.id) != null
        )
        assertNull(
            opponent.creature
                .get(outer.id)
        )
        assertEquals(
            before + 1,
            stack.remaining
        )
    }

    @Test
    fun noOpponentPlantTargets_resolvesWithoutAskingDecision() {
        val strategy =
            FailIfAskedStrategy()

        val actor =
            EffectTestFixture.player(
                id = 1,
                effectStrategy = strategy
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE
            ),
            nested
        )

        assertEquals(
            0,
            strategy.calls
        )
    }

    private fun graft(
        game: Game,
        player: Player,
        type: PlantType
    ): CreatureCard {
        val stack =
            game.grove.plantMarket
                .stacks
                .first {
                    it.card.type == type &&
                        it.isNotEmpty
                }

        val card =
            checkNotNull(
                stack.take()
            )

        return player.creature.graft(
            card,
            player.creature
                .legalPlacements(card)
                .first()
        )
    }

    private fun graftRoot(
        game: Game,
        player: Player
    ): CreatureCard =
        graft(
            game,
            player,
            PlantType.ROOT
        )

    private fun graftRootOutside(
        game: Game,
        player: Player,
        inner: CreatureCard
    ): CreatureCard {
        val stack =
            game.grove.plantMarket
                .stackFor(
                    inner.card
                )!!

        val card =
            checkNotNull(
                stack.take()
            )

        val placement =
            player.creature
                .legalPlacements(card)
                .first {
                    it.side ==
                        inner.side &&
                        (
                            inner.side ==
                                CreatureSide.LEFT &&
                                it.position.x <
                                inner.position.x ||
                            inner.side ==
                                CreatureSide.RIGHT &&
                                it.position.x >
                                inner.position.x
                            )
                }

        return player.creature.graft(
            card,
            placement
        )
    }

    private class ChooseOpponentPlantStrategy :
        FirstEffectChoiceStrategy() {

        var targetOwner:
            PlayerId? = null

        var targetCard:
            CreatureCardId? = null

        var offered:
            List<EffectOpponentPlantWoundChoice> =
            emptyList()

        override fun chooseOpponentPlantWound(
            request:
            ChooseEffectOpponentPlantWoundRequest
        ): EffectOpponentPlantWoundChoice {
            offered =
                request.legalChoices

            val owner =
                targetOwner
            val card =
                targetCard

            return request
                .legalChoices
                .first {
                    (
                        owner == null ||
                            it.ownerId == owner
                        ) &&
                        (
                            card == null ||
                                it.cardId == card
                            )
                }
        }
    }

    private class IllegalFaceDownTargetStrategy :
        FirstEffectChoiceStrategy() {

        lateinit var illegalChoice:
            EffectOpponentPlantWoundChoice

        override fun chooseOpponentPlantWound(
            request:
            ChooseEffectOpponentPlantWoundRequest
        ): EffectOpponentPlantWoundChoice =
            illegalChoice
    }

    private class FailIfAskedStrategy :
        FirstEffectChoiceStrategy() {

        var calls = 0

        override fun chooseOpponentPlantWound(
            request:
            ChooseEffectOpponentPlantWoundRequest
        ): EffectOpponentPlantWoundChoice {
            calls++
            error(
                "Strategy should not be asked when no legal targets exist"
            )
        }
    }
}
