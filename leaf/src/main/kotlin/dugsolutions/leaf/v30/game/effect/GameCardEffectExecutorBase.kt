package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.game.effect.details.UpgradeDie
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.player.decision.domain.MainActionBattle
import dugsolutions.leaf.v30.player.decision.domain.MainActionCultivation
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieSides
import dugsolutions.leaf.v30.random.die.di.DieFactory
import dugsolutions.leaf.v30.table.Table

abstract class GameCardEffectExecutorBase(
    protected val chronicle: Chronicle = GameChronicle(),
    private val dieFactory: DieFactory = DieFactory(Randomizer.create())
) {

    companion object {
        const val MIN_REROLL_VALUE = 3
        const val MAX_REROLL_ATTEMPTS = 10
        const val RAISE_PLUS_ONE = 1
    }

    protected open fun gainWormAndBoostWorms(
        table: Table,
        player: Player,
        action: MainActionCultivation.ExecuteCard
    ) {
        gainWormAndBoostWorms(table, player, action.card)
    }

    protected open fun gainWormAndBoostWorms(
        table: Table,
        player: Player,
        action: MainActionBattle.ExecuteCard
    ) {
        gainWormAndBoostWorms(table, player, action.card)
    }

    private fun gainWormAndBoostWorms(
        table: Table,
        player: Player,
        card: GameCard
    ) {
        if (table.grove.has(Critter.WORM)) {
            table.grove.remove(Critter.WORM)
            player.addCritter(Critter.WORM)
        }
        player.replaceCritter(Critter.WORM, Critter.BOOSTED_WORM)
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Gained a worm from the Grove and boosted this player's worms for the round",
                critter = Critter.BOOSTED_WORM
            )
        )
    }

    protected open fun mulchDieFromDiscard(
        table: Table,
        player: Player,
        action: MainActionCultivation.ExecuteCard
    ) {
        mulchDieFromDiscard(table, player, action.card)
    }

    protected open fun mulchDieFromDiscard(
        table: Table,
        player: Player,
        action: MainActionBattle.ExecuteCard
    ) {
        mulchDieFromDiscard(table, player, action.card)
    }

    protected open fun mulchDieFromDiscard(
        table: Table,
        player: Player,
        card: GameCard
    ) {
        val groveToken = table.grove.remove(Token.MULCH()) ?: return
        val die = player.drawHighestDieFromDiscard()
        if (die == null) {
            table.grove.add(groveToken)
            return
        }
        val token = Token.MULCH(DieSides.from(die.sides))
        player.add(token)
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Mulched the highest-sided die from discard and gained a matching mulch token",
                dice = Dice(listOf(die)),
                token = token
            )
        )
    }

    protected fun rerollUntilThreeOrHigher(
        initial: Die,
        reroll: (Die) -> Die?
    ): Die {
        var current = initial
        repeat(MAX_REROLL_ATTEMPTS) {
            current = reroll(current)
                ?: throw MainActionException("Reroll target die was not found")
            if (current.value >= MIN_REROLL_VALUE) {
                return current
            }
        }
        throw MainActionException("Reroll did not reach $MIN_REROLL_VALUE after $MAX_REROLL_ATTEMPTS attempts")
    }

    protected fun raiseDiePlus1AndGainWater(
        table: Table,
        player: Player,
        card: GameCard,
        die: Die,
        raiseDie: (Die, Int) -> Die?
    ) {
        val raisedDie = raiseDie(die, RAISE_PLUS_ONE) ?: return
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Raised a die by $RAISE_PLUS_ONE",
                dice = Dice(listOf(raisedDie))
            )
        )

        val token = table.grove.remove(Token.WATER) ?: return
        player.add(token)
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Gained a water token from the Grove",
                token = token
            )
        )
    }

    protected open fun upgradeDieAndUseNow(
        table: Table,
        player: Player,
        action: MainActionCultivation.ExecuteCard
    ): Die? {
        return upgradeDieAndUseNow(table, player, action.card, action.target)
    }

    protected open fun upgradeDieAndUseNow(
        table: Table,
        player: Player,
        action: MainActionBattle.ExecuteCard
    ): Die? {
        return upgradeDieAndUseNow(table, player, action.card, action.target)
    }

    private fun upgradeDieAndUseNow(
        table: Table,
        player: Player,
        card: GameCard,
        target: ExecuteTarget?
    ): Die? {
        val targetDie = (target as? ExecuteTarget.PlayerDie)?.dice?.firstDie
        if (targetDie == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_TARGET_MISSING, card = card))
            return null
        }
        if (!player.diceHand.hasDie(targetDie)) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_NOT_FOUND, card = card))
            return null
        }

        val upgraded = UpgradeDie(table.grove, dieFactory)(targetDie)
        if (upgraded == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_UNAVAILABLE, card = card))
            return null
        }
        if (!player.removeDieFromHand(targetDie)) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_NOT_FOUND, card = card))
            return null
        }
        upgraded.roll()
        player.addDieToHand(upgraded)
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Upgraded a die and used it now",
                dice = Dice(listOf(upgraded))
            )
        )
        return upgraded
    }

}
