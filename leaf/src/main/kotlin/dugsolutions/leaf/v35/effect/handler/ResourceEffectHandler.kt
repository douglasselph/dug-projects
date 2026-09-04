package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.player.decision.reward.ChooseCritterRequest
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.Token

/**
 * Effects that gain, spend, or attach shared resources/components.
 *
 * Simple fixed-resource effects live here. Effects involving strategic target
 * selection across opponents, temporary round-wide boosts, or complex limits
 * remain intentionally unsupported until their dedicated behavior is built.
 */
class ResourceEffectHandler : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        when (request.effect) {
            GameEffect.GAIN_WATER_TOKEN ->
                request.game.grove.tokens.hasWater

            GameEffect.GAIN_WATER_AND_SPEND_3_TO_REROLL_BATTLE_DIE ->
                request.phase == GameEffectPhase.CULTIVATION &&
                    request.game.grove.tokens.hasWater

            GameEffect.MULCH_DIE_FROM_HAND ->
                request.actor.dice.hand.isNotEmpty() &&
                    hasEmptyMulch(request)

            GameEffect.MULCH_DIE_FROM_DISCARD,
            GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD ->
                request.actor.dice.discard.isNotEmpty() &&
                    hasEmptyMulch(request)

            GameEffect.GAIN_ONE_VP ->
                true

            GameEffect.GAIN_ANY_TWO_CRITTERS ->
                availableNormalCritters(request).isNotEmpty()

            GameEffect.GAIN_TWO_WORMS ->
                request.game.grove.critters.count(Critter.WORM) > 0

            GameEffect.GAIN_ONE_WISP ->
                !request.game.grove.wispDeck.isEmpty

            GameEffect.GAIN_D10_TO_DISCARD ->
                request.game.grove.graftBed.has(DieSides.D10)

            GameEffect.GAIN_D12_TO_DISCARD ->
                request.game.grove.graftBed.has(DieSides.D12)

            GameEffect.GAIN_D20_TO_DISCARD ->
                request.game.grove.graftBed.has(DieSides.D20)

            GameEffect.GAIN_OR_REFRESH_GREEN_BUTTERFLY ->
                butterflyAvailable(request, Butterfly.GREEN)

            GameEffect.GAIN_OR_REFRESH_PURPLE_BUTTERFLY ->
                butterflyAvailable(request, Butterfly.PURPLE)

            GameEffect.GAIN_OR_REFRESH_RED_BUTTERFLY ->
                butterflyAvailable(request, Butterfly.RED)

            GameEffect.GAIN_OR_REFRESH_YELLOW_BUTTERFLY ->
                butterflyAvailable(request, Butterfly.YELLOW)

            else -> false
        }

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        check(canExecute(request)) {
            "Resource handler cannot execute effect: ${request.effect}"
        }

        when (request.effect) {
            GameEffect.GAIN_WATER_TOKEN,
            GameEffect.GAIN_WATER_AND_SPEND_3_TO_REROLL_BATTLE_DIE ->
                gainWater(request)

            GameEffect.MULCH_DIE_FROM_HAND ->
                mulchFromHand(request)

            GameEffect.MULCH_DIE_FROM_DISCARD,
            GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD ->
                mulchFromDiscard(request)

            GameEffect.GAIN_ONE_VP ->
                request.actor.addVp(1)

            GameEffect.GAIN_ANY_TWO_CRITTERS ->
                gainChosenCritters(request, 2)

            GameEffect.GAIN_TWO_WORMS ->
                gainWorms(request, 2)

            GameEffect.GAIN_ONE_WISP ->
                gainOneWisp(request, executor)

            GameEffect.GAIN_D10_TO_DISCARD ->
                gainFixedDie(request, DieSides.D10)

            GameEffect.GAIN_D12_TO_DISCARD ->
                gainFixedDie(request, DieSides.D12)

            GameEffect.GAIN_D20_TO_DISCARD ->
                gainFixedDie(request, DieSides.D20)

            GameEffect.GAIN_OR_REFRESH_GREEN_BUTTERFLY ->
                gainOrRefreshButterfly(request, Butterfly.GREEN)

            GameEffect.GAIN_OR_REFRESH_PURPLE_BUTTERFLY ->
                gainOrRefreshButterfly(request, Butterfly.PURPLE)

            GameEffect.GAIN_OR_REFRESH_RED_BUTTERFLY ->
                gainOrRefreshButterfly(request, Butterfly.RED)

            GameEffect.GAIN_OR_REFRESH_YELLOW_BUTTERFLY ->
                gainOrRefreshButterfly(request, Butterfly.YELLOW)

            else -> error(
                "Unsupported effect reached ResourceEffectHandler: ${request.effect}"
            )
        }
    }

    private fun gainWater(
        request: GameEffectRequest
    ) {
        val token = checkNotNull(
            request.game.grove.tokens.pull(Token.WATER)
        ) {
            "Validated Water effect could not take Water from Grove"
        }
        request.actor.tokens.add(token)
    }

    private fun mulchFromHand(
        request: GameEffectRequest
    ) {
        val die = chooseRequiredHandDie(
            request = request,
            legalChoices = handChoices(request.actor)
        )
        storeOnPendingMulch(request, die, fromDiscard = false)
    }

    private fun mulchFromDiscard(
        request: GameEffectRequest
    ) {
        val die = chooseRequiredDiscardDie(
            request = request,
            legalChoices = discardChoices(request.actor)
        )
        storeOnPendingMulch(request, die, fromDiscard = true)
    }

    private fun storeOnPendingMulch(
        request: GameEffectRequest,
        die: dugsolutions.leaf.v35.random.die.Die,
        fromDiscard: Boolean
    ) {
        val emptyMulch = request.game.grove.tokens.mulchTokens
            .firstOrNull { it.sides == null }
        check(emptyMulch != null) {
            "Validated Mulch effect has no empty Mulch token in Grove"
        }

        val removed = if (fromDiscard) {
            request.actor.dice.removeFromDiscard(die)
        } else {
            request.actor.dice.removeFromHand(die)
        }
        check(removed != null) {
            "Validated Mulch die could not be removed from player dice: $die"
        }
        check(request.game.grove.tokens.pull(emptyMulch) != null) {
            "Validated empty Mulch token could not be removed from Grove"
        }

        request.actor.tokens.add(
            Token.PENDING_MULCH(DieSides.from(die.sides))
        )
    }


    private fun gainChosenCritters(
        request: GameEffectRequest,
        count: Int
    ) {
        repeat(count) {
            val legalChoices = availableNormalCritters(request)
            if (legalChoices.isEmpty()) return

            val chosen = request.actor.decisions.reward.chooseCritter(
                ChooseCritterRequest(
                    legalChoices = legalChoices,
                    ownedCritters = request.actor.critters.all
                )
            )
            check(chosen in legalChoices) {
                "RewardStrategy returned illegal Critter choice for effect: " +
                    "$chosen; legal=$legalChoices"
            }
            check(request.game.grove.critters.remove(chosen)) {
                "Chosen Critter was no longer available in Grove: $chosen"
            }
            request.actor.critters.add(chosen)
        }
    }

    private fun gainWorms(
        request: GameEffectRequest,
        count: Int
    ) {
        repeat(count) {
            if (!request.game.grove.critters.remove(Critter.WORM)) return
            request.actor.critters.add(Critter.WORM)
        }
    }

    private fun availableNormalCritters(
        request: GameEffectRequest
    ): List<Critter> =
        listOf(Critter.BEE, Critter.WORM).filter {
            request.game.grove.critters.count(it) > 0
        }

    private fun gainOneWisp(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        val card = checkNotNull(request.game.grove.wispDeck.draw()) {
            "Validated Wisp gain found an empty Wisp deck"
        }

        if (card.playImmediately) {
            executor.execute(
                GameEffectRequest(
                    game = request.game,
                    actor = request.actor,
                    effect = card.effect,
                    source = GameEffectSource.Wisp(card),
                    phase = request.phase
                )
            )
        } else {
            request.actor.wisps.add(card)
        }
    }

    private fun gainFixedDie(
        request: GameEffectRequest,
        sides: DieSides
    ) {
        check(request.game.grove.graftBed.take(sides)) {
            "Validated gained die was no longer available: $sides"
        }
        request.actor.dice.addToDiscard(
            request.game.dieFactory(sides)
        )
    }

    private fun butterflyAvailable(
        request: GameEffectRequest,
        butterfly: Butterfly
    ): Boolean =
        butterfly in request.actor.butterflies.all ||
            butterfly in request.game.grove.butterflies.all ||
            request.game.players.any { player ->
                player !== request.actor &&
                    butterfly in player.butterflies.all
            }

    private fun gainOrRefreshButterfly(
        request: GameEffectRequest,
        butterfly: Butterfly
    ) {
        if (butterfly in request.actor.butterflies.all) {
            check(request.actor.butterflies.faceUp(butterfly)) {
                "Owned Butterfly could not be refreshed: $butterfly"
            }
            return
        }

        val previousOwner = request.game.players.firstOrNull { player ->
            player !== request.actor &&
                butterfly in player.butterflies.all
        }

        if (previousOwner != null) {
            check(previousOwner.butterflies.remove(butterfly)) {
                "Butterfly could not be removed from previous owner: $butterfly"
            }
        } else {
            check(request.game.grove.butterflies.remove(butterfly)) {
                "Butterfly could not be removed from Grove: $butterfly"
            }
        }

        request.actor.butterflies.add(butterfly)
    }

    private fun hasEmptyMulch(
        request: GameEffectRequest
    ): Boolean =
        request.game.grove.tokens.mulchTokens.any { it.sides == null }
}
