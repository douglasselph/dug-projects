package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.effect.DefaultGameEffectExecutor
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FirstEffectChoiceStrategy
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.decision.effect.ChooseOEdelweissRequest
import dugsolutions.leaf.v35.player.decision.effect.OEdelweissChoice
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OEdelweissVariantsTest {
    private val spent = GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE
    private val any = GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE

    @Test
    fun spentPlantCannotPlayUntilFlippedUp() {
        val strategy = ChooseTarget(OEdelweissChoiceType.FLIP_THEN_PLAY)
        val (actor, game, target, source, die) = setup(spent, strategy, faceUp = false)
        run(spent, actor, game, source)
        assertEquals(5, die.value)
        assertFalse(strategy.offered[0].any { it is OEdelweissChoice.Play && it.card.cardId == target })
        assertTrue(strategy.offered[1].any { it is OEdelweissChoice.Play && it.card.cardId == target })
        assertTrue(actor.creature.get(target)!!.isFaceDown)
    }

    @Test
    fun playingFaceUpPlantSpendsItAndCannotPlayItTwice() {
        val strategy = ChooseTarget(OEdelweissChoiceType.PLAY_THEN_DONE)
        val (actor, game, target, source, die) = setup(any, strategy, faceUp = true)
        run(any, actor, game, source)
        assertEquals(5, die.value)
        assertTrue(strategy.offered[0].any { it is OEdelweissChoice.Play && it.card.cardId == target })
        assertFalse(strategy.offered[1].any { it is OEdelweissChoice.Play && it.card.cardId == target })
        assertTrue(actor.creature.get(target)!!.isFaceDown)
    }

    @Test
    fun doneStopsEarlyAndSourceIsNeverOffered() {
        val strategy = ChooseTarget(OEdelweissChoiceType.DONE)
        val (actor, game, target, source, die) = setup(spent, strategy, faceUp = true)
        run(spent, actor, game, source)
        assertEquals(1, strategy.offered.size)
        assertEquals(1, die.value)
        assertTrue(actor.creature.get(target)!!.isFaceUp)
        assertTrue(strategy.offered.single().none {
            when (it) {
                is OEdelweissChoice.Play -> it.card.cardId == source.id
                is OEdelweissChoice.Flip -> it.card.cardId == source.id
                OEdelweissChoice.Done -> false
            }
        })
    }

    private fun setup(effect: GameEffect, strategy: ChooseTarget, faceUp: Boolean): Setup {
        val die = FixedEffectDie(8, 1)
        val actor = EffectTestFixture.player(1, hand = listOf(die), effectStrategy = strategy)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val target = RecursivePlantEffectTestFixture.graft(actor,
            RecursivePlantEffectTestFixture.plant("TargetVine", PlantType.VINE, GameEffect.RAISE_DIE_PLUS_4))
        val source = RecursivePlantEffectTestFixture.graft(actor,
            RecursivePlantEffectTestFixture.plant("OEdelweiss", PlantType.FLOWER, effect))
        if (faceUp) RecursivePlantEffectTestFixture.faceUp(actor, target)
        val currentSource = RecursivePlantEffectTestFixture.faceUp(actor, source)
        strategy.target = target.id
        return Setup(actor, game, target.id, currentSource, die)
    }

    private fun run(effect: GameEffect, actor: dugsolutions.leaf.v35.player.Player,
        game: dugsolutions.leaf.v35.game.Game, source: dugsolutions.leaf.v35.player.creature.CreatureCard) {
        DefaultGameEffectExecutor().execute(EffectTestFixture.request(game = game, actor = actor,
            effect = effect, source = GameEffectSource.Plant(source)))
    }

    private data class Setup(val actor: dugsolutions.leaf.v35.player.Player,
        val game: dugsolutions.leaf.v35.game.Game, val target: CreatureCardId,
        val source: dugsolutions.leaf.v35.player.creature.CreatureCard,
        val die: FixedEffectDie)

    private enum class OEdelweissChoiceType { FLIP_THEN_PLAY, PLAY_THEN_DONE, DONE }
    private class ChooseTarget(val mode: OEdelweissChoiceType): FirstEffectChoiceStrategy() {
        var target: CreatureCardId? = null
        val offered = mutableListOf<List<OEdelweissChoice>>()
        override fun chooseOEdelweiss(request: ChooseOEdelweissRequest): OEdelweissChoice {
            offered += request.legalChoices
            if (mode == OEdelweissChoiceType.DONE || (mode == OEdelweissChoiceType.PLAY_THEN_DONE && request.choiceNumber == 2)) return OEdelweissChoice.Done
            val targetId = requireNotNull(target) { "ChooseTarget.target must be initialized before selecting" }
            val flip = mode == OEdelweissChoiceType.FLIP_THEN_PLAY && request.choiceNumber == 1
            return if (flip) request.legalChoices.filterIsInstance<OEdelweissChoice.Flip>().first { it.card.cardId == targetId }
            else request.legalChoices.filterIsInstance<OEdelweissChoice.Play>().first { it.card.cardId == targetId }
        }
    }
}
