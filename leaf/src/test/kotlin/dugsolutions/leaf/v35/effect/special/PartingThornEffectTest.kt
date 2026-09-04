package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.error.InvalidDecisionException
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FirstEffectChoiceStrategy
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.effect.ChooseOptionalEffectPlantRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectPlantChoice
import dugsolutions.leaf.v35.player.decision.wound.ChooseWoundRequest
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice
import dugsolutions.leaf.v35.player.decision.wound.WoundStrategy
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PartingThornEffectTest {

    private val effect = PartingThornEffect()
    private val nested = GameEffectExecutor { }

    @Test
    fun cultivationDecisionSeesAllOwnedPlants_andCanFlipChosenFaceDownPlantUp() {
        val strategy = ChoosePlantStrategy()
        val actor = EffectTestFixture.player(1, effectStrategy = strategy)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        val first = graft(game, actor, PlantType.ROOT)
        val second = graft(game, actor, PlantType.VINE)
        actor.creature.faceUp(first.id)
        strategy.target = second.id

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.FLIP_OWN_PLANT_OR_WOUND_EACH_OPPONENT_IN_BATTLE
            ),
            nested
        )

        assertEquals(
            setOf(first.id, second.id),
            strategy.offered.map { it.cardId }.toSet()
        )
        assertTrue(actor.creature.get(first.id)!!.isFaceUp)
        assertTrue(actor.creature.get(second.id)!!.isFaceUp)
    }

    @Test
    fun cultivationMayDeclineToFlipAnyPlant() {
        val strategy = ChoosePlantStrategy(decline = true)
        val actor = EffectTestFixture.player(1, effectStrategy = strategy)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val card = graft(game, actor, PlantType.ROOT)
        actor.creature.faceUp(card.id)

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.FLIP_OWN_PLANT_OR_WOUND_EACH_OPPONENT_IN_BATTLE
            ),
            nested
        )

        assertTrue(actor.creature.get(card.id)!!.isFaceUp)
    }

    @Test
    fun cultivationIllegalPlantTarget_isRejectedBeforeMutation() {
        val strategy = ChoosePlantStrategy(illegal = true)
        val actor = EffectTestFixture.player(1, effectStrategy = strategy)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val card = graft(game, actor, PlantType.ROOT)
        actor.creature.faceUp(card.id)

        assertFailsWith<InvalidDecisionException> {
            effect.execute(
                EffectTestFixture.request(
                    game,
                    actor,
                    GameEffect.FLIP_OWN_PLANT_OR_WOUND_EACH_OPPONENT_IN_BATTLE
                ),
                nested
            )
        }

        assertTrue(actor.creature.get(card.id)!!.isFaceUp)
    }

    @Test
    fun battleForcesOneNormalWoundOnEveryOpponentButNotActor() {
        val actor = Player(PlayerId(1), DecisionDirector.baseline())
        val wound2 = RecordingWoundStrategy()
        val wound3 = RecordingWoundStrategy()
        val opponent2 = Player(
            PlayerId(2),
            DecisionDirector.baseline().copy(wound = wound2)
        )
        val opponent3 = Player(
            PlayerId(3),
            DecisionDirector.baseline().copy(wound = wound3)
        )
        val game = EffectTestFixture.game(actor, opponent2, opponent3)

        val actorCard = graft(game, actor, PlantType.ROOT)
        val card2 = graft(game, opponent2, PlantType.ROOT)
        val card3 = graft(game, opponent3, PlantType.VINE)
        actor.creature.faceUp(actorCard.id)
        opponent2.creature.faceUp(card2.id)
        opponent3.creature.faceUp(card3.id)

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.FLIP_OWN_PLANT_OR_WOUND_EACH_OPPONENT_IN_BATTLE
            ).copy(phase = GameEffectPhase.BATTLE),
            nested
        )

        assertTrue(actor.creature.get(actorCard.id)!!.isFaceUp)
        assertTrue(opponent2.creature.get(card2.id)!!.isFaceDown)
        assertTrue(opponent3.creature.get(card3.id)!!.isFaceDown)
        assertEquals(1, wound2.calls)
        assertEquals(1, wound3.calls)

        val woundMarkers = game.chronicle.entries
            .filterIsInstance<GameEntry.Marker>()
            .map { it.message }
            .filter { it.startsWith("WOUND ") }
        assertEquals(2, woundMarkers.size)
    }

    @Test
    fun battleUsesFullWoundResolverIncludingSnipAndReturnToGrove() {
        val actor = Player(PlayerId(1), DecisionDirector.baseline())
        val opponent = Player(PlayerId(2), DecisionDirector.baseline())
        val game = EffectTestFixture.game(actor, opponent)
        val card = graft(game, opponent, PlantType.ROOT)
        val stack = game.grove.plantMarket.stackFor(card.card)!!
        val before = stack.remaining

        // Grafted cards begin face down, so the Wound must Snip rather than Flip.
        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.FLIP_OWN_PLANT_OR_WOUND_EACH_OPPONENT_IN_BATTLE
            ).copy(phase = GameEffectPhase.BATTLE),
            nested
        )

        assertNull(opponent.creature.get(card.id))
        assertEquals(before + 1, stack.remaining)
    }

    private fun graft(
        game: Game,
        player: Player,
        type: PlantType
    ): CreatureCard {
        val stack = game.grove.plantMarket.stacks.first {
            it.card.type == type && it.isNotEmpty
        }
        val card = checkNotNull(stack.take())
        val placement = player.creature.legalPlacements(card).first()
        return player.creature.graft(card, placement)
    }

    private class ChoosePlantStrategy(
        private val decline: Boolean = false,
        private val illegal: Boolean = false
    ) : FirstEffectChoiceStrategy() {
        var target: CreatureCardId? = null
        var offered: List<EffectPlantChoice> = emptyList()

        override fun chooseOptionalPlant(
            request: ChooseOptionalEffectPlantRequest
        ): EffectPlantChoice? {
            offered = request.legalChoices
            if (decline) return null
            if (illegal) {
                return EffectPlantChoice(
                    cardId = CreatureCardId(999),
                    cardName = "Foreign",
                    isFaceUp = true
                )
            }
            return target?.let { targetId ->
                request.legalChoices.first { it.cardId == targetId }
            } ?: request.legalChoices.firstOrNull()
        }
    }

    private class RecordingWoundStrategy : WoundStrategy {
        var calls = 0

        override fun choose(request: ChooseWoundRequest): WoundChoice {
            calls++
            return request.legalChoices.first()
        }
    }
}
