package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.DefaultGameEffectExecutor
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class R3B1ResearchEffectIsolationTest {
    @Test fun `only designated actor receives changed Plant effect and records output`() {
        val aDie = FixedEffectDie(20, 2)
        val bDie = FixedEffectDie(20, 2)
        val a = EffectTestFixture.player(1,hand=listOf(aDie))
        val b = EffectTestFixture.player(2,hand=listOf(bDie))
        val initial = EffectTestFixture.game(a,b)
        val card = initial.grove.plantMarket.stacks.first().card.copy(effect=GameEffect.RAISE_DIE_PLUS_4)
        val plant = CreatureCard(CreatureCardId(7),card,CreatureSide.LEFT,CreaturePosition(-1,0))
        val config = GameConfig(playerDecisionFactories=List(2) { PlayerDecisionFactory.humanBaseline() },
            researchPlantEffectsBySeat=mapOf(0 to mapOf(card.name to GameEffect.RESEARCH_GAIN_TWENTY_VP)),
            recordResearchEffectOutputs=true)
        val game = Game(config,initial.grove,listOf(a,b),initial.chronicle,initial.roundDeck,initial.randomizer)
        val executor = DefaultGameEffectExecutor()
        fun request(actor: dugsolutions.leaf.v35.player.Player) = GameEffectRequest(
            game=game,actor=actor,effect=GameEffect.RAISE_DIE_PLUS_4,
            source=GameEffectSource.Plant(plant),phase=GameEffectPhase.CULTIVATION)
        assertEquals(GameEffect.RESEARCH_GAIN_TWENTY_VP,game.effectFor(a,card))
        assertEquals(GameEffect.RAISE_DIE_PLUS_4,game.effectFor(b,card))
        executor.execute(request(a))
        executor.execute(request(b))
        assertEquals(20,a.vp)
        assertEquals(0,b.vp)
        assertEquals(2,aDie.value)
        assertEquals(6,bDie.value)
        assertEquals(2,game.researchEffectOutputs.size)
        val aOutput=game.researchEffectOutputs.single { it.playerSeat==0 }
        val bOutput=game.researchEffectOutputs.single { it.playerSeat==1 }
        assertEquals(20,aOutput.immediateVpDelta)
        assertEquals(0,aOutput.existingHandDieValueDelta)
        assertEquals(0,bOutput.immediateVpDelta)
        assertEquals(4,bOutput.existingHandDieValueDelta)
    }
    @Test fun `research effect outputs require explicit opt in`() {
        val a=EffectTestFixture.player(1,hand=listOf(FixedEffectDie(20,1)))
        val game=EffectTestFixture.game(a,EffectTestFixture.player(2))
        DefaultGameEffectExecutor().execute(EffectTestFixture.request(game,a,GameEffect.RESEARCH_GAIN_TWENTY_VP))
        assertEquals(20,a.vp)
        assertTrue(game.researchEffectOutputs.isEmpty())
    }
}
