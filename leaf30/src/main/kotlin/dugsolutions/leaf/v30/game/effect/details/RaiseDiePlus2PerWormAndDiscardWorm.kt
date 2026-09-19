package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.game.effect.scope.DieEffectScope
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice

class RaiseDiePlus2PerWormAndDiscardWorm(
    private val chronicle: Chronicle
) {

    private companion object {
        const val RAISE_PER_WORM = 2
        const val DISCARD_WORM_THRESHOLD = 3
    }

    operator fun invoke(
        scope: DieEffectScope,
        grove: Grove,
        player: Player,
        card: GameCard,
        target: ExecuteTarget?
    ) {
        val targetDie = target?.dice?.firstDie
        if (targetDie == null) {
            chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.RAISE_TARGET_MISSING, card = card))
            return
        }
        if (!scope.hasDie(targetDie)) {
            chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.RAISE_DIE_NOT_FOUND, card = card))
            return
        }

        val wormCount = player.critters.count { it.normal == Critter.WORM }
        val raised = scope.raise(targetDie, wormCount * RAISE_PER_WORM) ?: return
        var discarded: Critter? = null
        if (wormCount >= DISCARD_WORM_THRESHOLD) {
            discarded = discardOneWorm(player)
            if (discarded != null) {
                grove.add(Critter.WORM)
            }
        }

        chronicle(
            Moment.GameCardEffect(
                player = scope.actingPlayer,
                card = card,
                effect = card.effect,
                detail = buildDetail(scope.locationDescription, wormCount, discarded),
                dice = Dice(listOf(raised)),
                critter = discarded
            )
        )
    }

    private fun discardOneWorm(player: Player): Critter? {
        if (player.removeCritter(Critter.WORM)) return Critter.WORM
        if (player.removeCritter(Critter.BOOSTED_WORM)) return Critter.BOOSTED_WORM
        return null
    }

    private fun buildDetail(
        location: String,
        wormCount: Int,
        discarded: Critter?
    ): String {
        return "Raised one die in $location by ${wormCount * RAISE_PER_WORM} from $wormCount worms" +
            if (discarded != null) " and discarded one worm" else ""
    }

}
