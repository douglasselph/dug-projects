package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.MainActionType
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.common.Butterfly
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.game.effect.details.GainButterfly
import dugsolutions.leaf.v30.game.effect.details.GainCritter
import dugsolutions.leaf.v30.game.effect.details.UpgradeDieTwice
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieSides
import dugsolutions.leaf.v30.random.die.di.DieFactory
import dugsolutions.leaf.v30.table.Table
import dugsolutions.leaf.v30.wisp.domain.WispCard
import dugsolutions.leaf.v30.wisp.domain.WispEffect

open class WispCardEffectExecutor(
    private val chronicle: Chronicle = GameChronicle(),
    private val dieFactory: DieFactory = DieFactory(Randomizer.create())
) {
    private companion object {
        const val CRITTER_GAIN_COUNT = 2
        const val SWAP_DICE_COUNT = 2
    }

    open operator fun invoke(
        table: Table,
        player: Player,
        card: WispCard
    ) {
        invoke(table, player, card, null)
    }

    open operator fun invoke(
        table: Table,
        player: Player,
        card: WispCard,
        target: ExecuteTarget?
    ) {
        when (card.effect) {
            WispEffect.UNKNOWN -> unknown(player, card.effect)
            WispEffect.KEEP_2_VP -> chronicleWisp(player, card, "Kept for 2 VP")
            WispEffect.GAIN_2_CRITTERS -> gainCritters(table, player, card, target)
            WispEffect.GAIN_MULCH -> gainMulch(player, card)
            WispEffect.GAIN_YELLOW_BUTTERFLY -> gainButterfly(table, player, card, Butterfly.YELLOW)
            WispEffect.GAIN_RED_BUTTERFLY -> gainButterfly(table, player, card, Butterfly.RED)
            WispEffect.GAIN_GREEN_BUTTERFLY -> gainButterfly(table, player, card, Butterfly.GREEN)
            WispEffect.GAIN_PURPLE_BUTTERFLY -> gainButterfly(table, player, card, Butterfly.PURPLE)
            WispEffect.SWAP_DICE -> swapDice(table, player, card, target)
            WispEffect.UPGRADE_2_STEPS -> upgradeTwoSteps(table, player, card, target)
        }
    }

    private fun unknown(
        player: Player,
        effect: WispEffect
    ) {
        chronicle(
            Moment.Warning(
                player = player,
                type = WarningType.UNKNOWN_EFFECT,
                detail = "WispEffect.$effect"
            )
        )
    }

    private fun gainCritters(
        table: Table,
        player: Player,
        card: WispCard,
        target: ExecuteTarget?
    ) {
        val requested = target?.critter.orEmpty()
        val gainCritter = GainCritter(table.grove)
        repeat(CRITTER_GAIN_COUNT) { index ->
            val critter = requested.getOrNull(index)?.normal ?: Critter.BEE
            gainCritter(critter)?.let { player.addCritter(it) }
        }
        chronicleWisp(player, card, "Gained up to $CRITTER_GAIN_COUNT critters")
    }

    private fun gainMulch(
        player: Player,
        card: WispCard
    ) {
        val die = player.drawHighestDieFromDiscard()
        if (die == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_NOT_FOUND, detail = "WispEffect.${card.effect}"))
            return
        }
        val token = Token.MULCH(DieSides.from(die.sides))
        player.add(token)
        chronicleWisp(player, card, "Mulched the highest-sided discard die", die, token)
    }

    private fun gainButterfly(
        table: Table,
        player: Player,
        card: WispCard,
        butterfly: Butterfly
    ) {
        GainButterfly(table)(player, butterfly)
        chronicleWisp(player, card, "Gained $butterfly butterfly face up")
    }

    private fun upgradeTwoSteps(
        table: Table,
        player: Player,
        card: WispCard,
        target: ExecuteTarget?
    ) {
        val die = target?.dice?.firstDie
        if (die == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_TARGET_MISSING, detail = "WispEffect.${card.effect}"))
            return
        }
        if (!player.removeDieFromHand(die)) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_NOT_FOUND, detail = "WispEffect.${card.effect}"))
            return
        }
        val upgraded = UpgradeDieTwice(table.grove, dieFactory)(die)
        if (upgraded == null) {
            player.addDieToHand(die)
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_UNAVAILABLE, detail = "WispEffect.${card.effect}"))
            return
        }
        upgraded.roll()
        player.addDieToHand(upgraded)
        chronicleWisp(player, card, "Upgraded one die two steps and used it now", upgraded)
    }

    private fun swapDice(
        table: Table,
        player: Player,
        card: WispCard,
        target: ExecuteTarget?
    ) {
        val targetPlayer = target?.player
        val dice = target?.dice?.diceInOrder.orEmpty()
        if (targetPlayer == null || dice.size != SWAP_DICE_COUNT) {
            chronicle(Moment.Warning(player = player, type = WarningType.RAISE_TARGET_MISSING, detail = "WispEffect.${card.effect}"))
            return
        }

        val swapped = if (target.row == null) {
            swapHandDice(player, targetPlayer, dice[0], dice[1])
        } else {
            swapBattleDice(table, player, targetPlayer, target.row, dice[0], dice[1])
        }
        if (swapped == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.RAISE_DIE_NOT_FOUND, detail = "WispEffect.${card.effect}"))
            return
        }
        chronicleWisp(player, card, "Swapped die values with player ${targetPlayer.id}", dice = Dice(swapped))
    }

    private fun swapHandDice(
        player: Player,
        targetPlayer: Player,
        ownRequested: Die,
        targetRequested: Die
    ): List<Die>? {
        val ownDie = player.diceHand.dice.firstOrNull { it == ownRequested } ?: return null
        val targetDie = targetPlayer.diceHand.dice.firstOrNull { it == targetRequested } ?: return null
        val ownValue = ownDie.value
        ownDie.adjustTo(targetDie.value)
        targetDie.adjustTo(ownValue)
        return listOf(ownDie, targetDie)
    }

    private fun swapBattleDice(
        table: Table,
        player: Player,
        targetPlayer: Player,
        targetRow: BattleStrikeRow,
        ownRequested: Die,
        targetRequested: Die
    ): List<Die>? {
        val ownSelection = BattleStrikeRow.entries
            .firstNotNullOfOrNull { row ->
                table.battle.grid.getSquare(player.id, row).all
                    .filterIsInstance<BattleItem.DieItem>()
                    .firstOrNull { it.die == ownRequested }
                    ?.let { row to it.die }
            }
            ?: return null
        val targetDie = table.battle.grid.getSquare(targetPlayer.id, targetRow).all
            .filterIsInstance<BattleItem.DieItem>()
            .firstOrNull { it.die == targetRequested }
            ?.die
            ?: return null
        val ownValue = ownSelection.second.value
        table.battle.setDieValue(player, ownSelection.first, ownSelection.second, targetDie.value)
        table.battle.setDieValue(targetPlayer, targetRow, targetDie, ownValue)
        return listOf(ownSelection.second, targetDie)
    }

    private fun chronicleWisp(
        player: Player,
        card: WispCard,
        detail: String,
        die: Die? = null,
        token: Token? = null,
        dice: Dice? = null
    ) {
        chronicle(
            Moment.MainAction(
                player = player,
                action = MainActionType.PLAY_WISP_CARD,
                detail = "Resolved ${card.effect}: $detail" + dice?.let { " dice=${it.values()}" }.orEmpty(),
                die = die,
                token = token,
                wispCard = card
            )
        )
    }
}
