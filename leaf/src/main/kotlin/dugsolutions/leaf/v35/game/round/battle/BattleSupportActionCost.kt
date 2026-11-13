package dugsolutions.leaf.v35.game.round.battle

import dugsolutions.leaf.v35.player.FreeBattleSupport
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.tokens.Critter

/** A free Plant-granted action consumes zero *paid slots*, not zero resources. */
object BattleSupportActionCost {
    fun paidSlots(player: Player, action: BattleSupportAction): Int =
        if (freeKind(action)?.let { player.freeBattleSupports.contains(it) } == true) 0 else 1

    fun freeKind(action: BattleSupportAction): FreeBattleSupport? = when (action) {
        is BattleSupportAction.PlaceCritter -> when (action.critter) {
            Critter.BEE -> FreeBattleSupport.BEE
            Critter.WORM -> FreeBattleSupport.WORM
        }
        is BattleSupportAction.Shared -> when (action.action) {
            is SupportAction.UseWormFlip -> FreeBattleSupport.WORM
            is SupportAction.UseButterfly -> FreeBattleSupport.BUTTERFLY
            else -> null
        }
        is BattleSupportAction.UseSunlight -> null
    }
}
