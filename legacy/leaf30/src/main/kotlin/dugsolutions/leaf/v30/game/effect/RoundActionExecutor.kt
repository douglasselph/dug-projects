package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.cards.domain.CardType
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.MainActionType
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.game.effect.details.GainCritter
import dugsolutions.leaf.v30.game.effect.details.GainGameCard
import dugsolutions.leaf.v30.game.effect.details.UpgradeDie
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ActionCultivation
import dugsolutions.leaf.v30.player.decision.domain.ActionRound
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieSides
import dugsolutions.leaf.v30.random.die.di.DieFactory
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.round.domain.RoundEffect
import dugsolutions.leaf.v30.round.domain.RoundEffect.*
import dugsolutions.leaf.v30.table.Table

open class RoundActionExecutor(
    private val chronicle: Chronicle = GameChronicle(),
    private val dieFactory: DieFactory = DieFactory(Randomizer.create())
) {
    private companion object {
        const val RAISE_BY_3_AMOUNT = 3
        const val VP_GAIN = 1
        const val WORM_GAIN_COUNT = 2
    }

    open operator fun invoke(
        table: Table,
        player: Player,
        card: RoundCard,
        action: ActionCultivation.DoRoundAction
    ) {
        invoke(table, player, card, action.actionRound, action.target)
    }

    open operator fun invoke(
        table: Table,
        player: Player,
        card: RoundCard,
        action: ActionRound
    ) {
        invoke(table, player, card, action, null)
    }

    open operator fun invoke(
        table: Table,
        player: Player,
        card: RoundCard,
        action: ActionRound,
        target: ExecuteTarget?
    ) {
        val effect = when (action) {
            ActionRound.ACTION_1 -> card.effect1
            ActionRound.ACTION_2 -> card.effect2
        }
        doEffectAction(table, player, card, effect, target)
    }

    private fun doEffectAction(
        table: Table,
        player: Player,
        card: RoundCard,
        effect: RoundEffect,
        target: ExecuteTarget?
    ) {
        when (effect) {
            UNKNOWN -> unknown(player, effect)
            RAISE_BY_3 -> raiseBy3(player, effect, target)
            GAIN_WATER -> gainWater(player, effect)
            GAIN_MULCH -> gainMulch(player, effect, target)
            UPGRADE_DIE -> upgradeDie(table, player, effect, target)
            GAIN_WORMS -> gainWorms(table, player, effect)
            GAIN_VP -> gainVp(player, effect)
            GAIN_ROOT -> gainGameCard(table, player, effect, CardType.ROOT)
            GAIN_VINE -> gainGameCard(table, player, effect, CardType.VINE)
            GAIN_FLOWER -> gainGameCard(table, player, effect, CardType.FLOWER)
            GAIN_D12 -> gainDie(table, player, effect, DieSides.D12)
            GAIN_D20 -> gainDie(table, player, effect, DieSides.D20)
        }
    }

    private fun unknown(
        player: Player,
        effect: RoundEffect
    ) {
        chronicle(
            Moment.Warning(
                player = player,
                type = WarningType.UNKNOWN_EFFECT,
                detail = "RoundEffect.$effect"
            )
        )
    }

    private fun raiseBy3(
        player: Player,
        effect: RoundEffect,
        target: ExecuteTarget?
    ) {
        val die = target?.dice?.firstDie
        if (die == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.RAISE_TARGET_MISSING, detail = "RoundEffect.$effect"))
            return
        }
        val raised = player.raiseDie(die, RAISE_BY_3_AMOUNT)
        if (raised == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.RAISE_DIE_NOT_FOUND, detail = "RoundEffect.$effect"))
            return
        }
        chronicleRoundAction(player, effect, "Raised one die by $RAISE_BY_3_AMOUNT", raised)
    }

    private fun gainWater(
        player: Player,
        effect: RoundEffect
    ) {
        player.add(Token.WATER)
        chronicleRoundAction(player, effect, "Gained water", token = Token.WATER)
    }

    private fun gainMulch(
        player: Player,
        effect: RoundEffect,
        target: ExecuteTarget?
    ) {
        val die = target?.dice?.firstDie
        if (die == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_TARGET_MISSING, detail = "RoundEffect.$effect"))
            return
        }
        if (!player.removeDieFromHand(die)) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_NOT_FOUND, detail = "RoundEffect.$effect"))
            return
        }
        val token = Token.PENDING_MULCH(DieSides.from(die.sides))
        player.add(token)
        chronicleRoundAction(player, effect, "Mulched one die from hand", die, token)
    }

    private fun upgradeDie(
        table: Table,
        player: Player,
        effect: RoundEffect,
        target: ExecuteTarget?
    ) {
        val die = target?.dice?.firstDie
        if (die == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_TARGET_MISSING, detail = "RoundEffect.$effect"))
            return
        }
        if (!player.removeDieFromHand(die)) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_NOT_FOUND, detail = "RoundEffect.$effect"))
            return
        }
        val upgraded = UpgradeDie(table.grove, dieFactory)(die)
        if (upgraded == null) {
            player.addDieToHand(die)
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_UNAVAILABLE, detail = "RoundEffect.$effect"))
            return
        }
        player.addDieToDiscard(upgraded)
        chronicleRoundAction(player, effect, "Upgraded one die into discard", upgraded)
    }

    private fun gainWorms(
        table: Table,
        player: Player,
        effect: RoundEffect
    ) {
        val gainCritter = GainCritter(table.grove)
        repeat(WORM_GAIN_COUNT) {
            gainCritter(Critter.WORM)?.let { player.addCritter(it) }
        }
        chronicleRoundAction(player, effect, "Gained up to $WORM_GAIN_COUNT worms")
    }

    private fun gainVp(
        player: Player,
        effect: RoundEffect
    ) {
        player.addVp(VP_GAIN)
        chronicleRoundAction(player, effect, "Gained $VP_GAIN VP")
    }

    private fun gainGameCard(
        table: Table,
        player: Player,
        effect: RoundEffect,
        type: CardType
    ) {
        val card = GainGameCard(table.grove)(type) ?: run {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_UNAVAILABLE, detail = "RoundEffect.$effect $type card unavailable"))
            return
        }
        player.addCardToCreature(card)
        chronicle(
            Moment.MainAction(
                player = player,
                action = MainActionType.DO_ROUND_ACTION,
                detail = "Resolved $effect and gained ${card.name} face down",
                card = card
            )
        )
    }

    private fun gainDie(
        table: Table,
        player: Player,
        effect: RoundEffect,
        sides: DieSides
    ) {
        if (!table.grove.remove(sides)) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_UNAVAILABLE, detail = "RoundEffect.$effect D${sides.value} unavailable"))
            return
        }
        val die = dieFactory(sides)
        player.addDieToDiscard(die)
        chronicleRoundAction(player, effect, "Gained D${sides.value} into discard", die)
    }

    private fun chronicleRoundAction(
        player: Player,
        effect: RoundEffect,
        detail: String,
        die: Die? = null,
        token: Token? = null
    ) {
        chronicle(
            Moment.MainAction(
                player = player,
                action = MainActionType.DO_ROUND_ACTION,
                detail = "Resolved $effect: $detail",
                die = die,
                token = token
            )
        )
    }
}
