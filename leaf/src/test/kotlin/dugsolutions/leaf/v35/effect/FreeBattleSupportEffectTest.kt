package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.game.round.battle.BattleSupportActionCost
import dugsolutions.leaf.v35.player.FreeBattleSupport
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Revised official CSV FREE clauses; FREE-1–4 deterministic mechanics. */
class FreeBattleSupportEffectTest {
    private val engine = DefaultGameEffectExecutor()

    @Test
    fun `Root Appreciation stacks plus one Worm value but permission remains boolean`() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val effect = GameEffect.GAIN_WORM_BOOST_AND_FREE_BATTLE_SUPPORT
        repeat(2) {
            assertTrue(engine.supports(effect))
            engine.execute(battleRequest(game, actor, effect))
        }
        assertEquals(3, actor.critterValues.valueOf(Critter.WORM))
        assertTrue(actor.freeBattleSupports.contains(FreeBattleSupport.WORM))
        assertEquals(0, BattleSupportActionCost.paidSlots(actor,
            BattleSupportAction.PlaceCritter(Critter.WORM, StrikeRow.TOP)))
        assertEquals(0, BattleSupportActionCost.paidSlots(actor,
            BattleSupportAction.Shared(SupportAction.UseWormFlip(CreatureCardId(1)))))
        assertEquals(1, BattleSupportActionCost.paidSlots(actor,
            BattleSupportAction.PlaceCritter(Critter.BEE, StrikeRow.TOP)))
        assertEquals(1, BattleSupportActionCost.paidSlots(actor,
            BattleSupportAction.UseSunlight(BattleMainAction.Draw)))
        actor.freeBattleSupports.clearBattle()
        assertFalse(actor.freeBattleSupports.contains(FreeBattleSupport.WORM))
        assertEquals(1, BattleSupportActionCost.paidSlots(actor,
            BattleSupportAction.PlaceCritter(Critter.WORM, StrikeRow.TOP)))
    }

    @Test
    fun `Bee-loved Bloom stacks plus two Bee value and grants only free Bee placement`() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val effect = GameEffect.GAIN_OR_STEAL_BEE_BOOST_AND_FREE_BATTLE_SUPPORT
        repeat(2) { engine.execute(battleRequest(game, actor, effect)) }
        assertEquals(6, actor.critterValues.valueOf(Critter.BEE))
        assertTrue(actor.freeBattleSupports.contains(FreeBattleSupport.BEE))
        assertEquals(0, BattleSupportActionCost.paidSlots(actor,
            BattleSupportAction.PlaceCritter(Critter.BEE, StrikeRow.MIDDLE)))
        assertEquals(1, BattleSupportActionCost.paidSlots(actor,
            BattleSupportAction.PlaceCritter(Critter.WORM, StrikeRow.MIDDLE)))
        assertFalse(actor.freeBattleSupports.contains(FreeBattleSupport.WORM))
    }

    @Test
    fun `Alluring Nectar grants only free ready Butterfly use`() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val effect = GameEffect.GAIN_OR_STEAL_BUTTERFLY_REFRESH_AND_FREE_BATTLE_SUPPORT
        assertTrue(engine.supports(effect))
        engine.execute(battleRequest(game, actor, effect))
        assertTrue(actor.freeBattleSupports.contains(FreeBattleSupport.BUTTERFLY))
        // No cost reduction for Water, Worm Flip, Sunlight or Wisp.
        val choice = dugsolutions.leaf.v35.player.decision.support.HandDieChoice(0, 6, 3)
        assertEquals(0, BattleSupportActionCost.paidSlots(actor,
            BattleSupportAction.Shared(SupportAction.UseButterfly(Butterfly.GREEN, choice))))
        assertEquals(1, BattleSupportActionCost.paidSlots(actor,
            BattleSupportAction.Shared(SupportAction.UseWaterRefresh)))
        actor.freeBattleSupports.clearBattle()
        assertEquals(1, BattleSupportActionCost.paidSlots(actor,
            BattleSupportAction.Shared(SupportAction.UseButterfly(Butterfly.GREEN, choice))))
    }

    @Test
    fun `Cultivation resolution boosts helpers but does not grant a future Battle discount`() {
        for ((effect, kind) in listOf(
            GameEffect.GAIN_WORM_BOOST_AND_FREE_BATTLE_SUPPORT to FreeBattleSupport.WORM,
            GameEffect.GAIN_OR_STEAL_BEE_BOOST_AND_FREE_BATTLE_SUPPORT to FreeBattleSupport.BEE,
            GameEffect.GAIN_OR_STEAL_BUTTERFLY_REFRESH_AND_FREE_BATTLE_SUPPORT to FreeBattleSupport.BUTTERFLY
        )) {
            val actor = EffectTestFixture.player(1)
            val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
            engine.execute(EffectTestFixture.request(game, actor, effect))
            assertFalse(actor.freeBattleSupports.contains(kind), "Cultivation must not grant $kind")
        }
    }

    private fun battleRequest(
        game: dugsolutions.leaf.v35.game.Game,
        actor: dugsolutions.leaf.v35.player.Player,
        effect: GameEffect
    ) = EffectTestFixture.request(game, actor, effect).copy(phase = GameEffectPhase.BATTLE)
}
