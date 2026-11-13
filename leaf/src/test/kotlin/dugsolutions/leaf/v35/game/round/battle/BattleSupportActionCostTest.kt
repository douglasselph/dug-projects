package dugsolutions.leaf.v35.game.round.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.FreeBattleSupport
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import kotlin.test.Test
import kotlin.test.assertEquals

/** FREE changes the Engagement budget, not ownership/consumption of physical resources. */
class BattleSupportActionCostTest {
    private fun player() = Player(PlayerId(1), DecisionDirector.mechanicalControl())

    @Test
    fun `each granted resource type becomes free but unrelated supports remain paid`() {
        val player = player()
        val bee = BattleSupportAction.PlaceCritter(Critter.BEE, StrikeRow.TOP)
        val worm = BattleSupportAction.PlaceCritter(Critter.WORM, StrikeRow.MIDDLE)
        val flip = BattleSupportAction.Shared(SupportAction.UseWormFlip(CreatureCardId(1)))
        val butterfly = BattleSupportAction.Shared(SupportAction.UseButterfly(
            Butterfly.GREEN, HandDieChoice(index = 0, sides = 6, value = 3)))
        val sunlight = BattleSupportAction.UseSunlight(BattleMainAction.Draw)
        val water = BattleSupportAction.Shared(SupportAction.UseWaterRefresh)
        val all = listOf(bee, worm, flip, butterfly, sunlight, water)

        assertEquals(listOf(1, 1, 1, 1, 1, 1), all.map { BattleSupportActionCost.paidSlots(player, it) })
        player.freeBattleSupports.grant(FreeBattleSupport.BEE)
        player.freeBattleSupports.grant(FreeBattleSupport.WORM)
        player.freeBattleSupports.grant(FreeBattleSupport.BUTTERFLY)
        player.freeBattleSupports.grant(FreeBattleSupport.BEE) // no stacking
        assertEquals(listOf(0, 0, 0, 0, 1, 1), all.map { BattleSupportActionCost.paidSlots(player, it) })
        player.freeBattleSupports.clearBattle()
        assertEquals(listOf(1, 1, 1, 1, 1, 1), all.map { BattleSupportActionCost.paidSlots(player, it) })
    }
}
