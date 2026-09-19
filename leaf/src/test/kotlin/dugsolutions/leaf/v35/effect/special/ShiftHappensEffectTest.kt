package dugsolutions.leaf.v35.effect.special

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
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectOpponentPlantWoundRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectPlantRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectOpponentPlantWoundChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectPlantChoice
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShiftHappensEffectTest {

    private val effect = ShiftHappensEffect()
    private val nested = GameEffectExecutor { }

    @Test
    fun cultivation_flipsChosenOwnedPlant() {
        val strategy = ShiftStrategy()
        val actor = EffectTestFixture.player(1, effectStrategy = strategy)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val first = graft(game, actor, PlantType.ROOT)
        val second = graft(game, actor, PlantType.VINE)
        actor.creature.faceUp(first.id)
        strategy.ownTarget = second.id

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.FLIP_OWN_PLANT_OR_FLIP_OPPONENT_ROOT_OR_VINE_IN_BATTLE
            ),
            nested
        )

        assertEquals(setOf(first.id, second.id), strategy.ownOffered.map { it.cardId }.toSet())
        assertTrue(actor.creature.get(first.id)!!.isFaceUp)
        assertTrue(actor.creature.get(second.id)!!.isFaceUp)
    }

    @Test
    fun battle_offersOnlyOpponentRootsAndVinesAndFlipsChosenCard() {
        val strategy = ShiftStrategy()
        val actor = EffectTestFixture.player(1, effectStrategy = strategy)
        val opponent = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, opponent)
        val root = graft(game, opponent, PlantType.ROOT)
        val vine = graft(game, opponent, PlantType.VINE)
        graft(game, opponent, PlantType.FLOWER)
        opponent.creature.faceUp(root.id)
        opponent.creature.faceUp(vine.id)
        strategy.opponentOwner = opponent.id
        strategy.opponentTarget = vine.id

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.FLIP_OWN_PLANT_OR_FLIP_OPPONENT_ROOT_OR_VINE_IN_BATTLE
            ).copy(phase = GameEffectPhase.BATTLE),
            nested
        )

        assertEquals(setOf(root.id, vine.id), strategy.opponentOffered.map { it.cardId }.toSet())
        assertTrue(opponent.creature.get(root.id)!!.isFaceUp)
        assertTrue(opponent.creature.get(vine.id)!!.isFaceDown)
    }

    private fun graft(game: Game, player: Player, type: PlantType): CreatureCard {
        val stack = game.grove.plantMarket.stacks.first {
            it.card.type == type && it.isNotEmpty
        }
        val card = checkNotNull(stack.take())
        val placement = player.creature.legalPlacements(card).first()
        return player.creature.graft(card, placement)
    }

    private class ShiftStrategy : FirstEffectChoiceStrategy() {
        var ownTarget: CreatureCardId? = null
        var ownOffered: List<EffectPlantChoice> = emptyList()
        var opponentOwner: PlayerId? = null
        var opponentTarget: CreatureCardId? = null
        var opponentOffered: List<EffectOpponentPlantWoundChoice> = emptyList()

        override fun choosePlantEffect(request: ChooseEffectPlantRequest): EffectPlantChoice {
            ownOffered = request.legalChoices
            return request.legalChoices.first { it.cardId == ownTarget }
        }

        override fun chooseOpponentPlantWound(
            request: ChooseEffectOpponentPlantWoundRequest
        ): EffectOpponentPlantWoundChoice {
            opponentOffered = request.legalChoices
            return request.legalChoices.first {
                it.ownerId == opponentOwner && it.cardId == opponentTarget
            }
        }
    }
}
