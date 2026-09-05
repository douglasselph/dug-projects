package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.error.stateNotNull
import dugsolutions.leaf.v35.error.unsupportedGameEffect
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieSizeRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectPlayerRequest
import dugsolutions.leaf.v35.player.decision.reward.ChooseCritterRequest
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.Token

/**
 * Effects that gain, spend, or attach shared resources/components.
 *
 * Simple fixed-resource effects live here. Player-local temporary Critter value
 * overrides also live here because they modify a gained shared resource without
 * changing the physical Critter identity. Effects involving strategic target
 * selection across opponents or complex limits remain separate.
 */
class ResourceEffectHandler : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        when (request.effect) {
            GameEffect.GAIN_WATER_TOKEN ->
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

            GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND ->
                true

            GameEffect.GAIN_ONE_WISP ->
                !request.game.grove.wispDeck.isEmpty

            GameEffect.GAIN_D10_TO_DISCARD ->
                request.game.grove.graftBed.has(DieSides.D10)

            GameEffect.GAIN_D12_TO_DISCARD ->
                request.game.grove.graftBed.has(DieSides.D12)

            GameEffect.GAIN_D20_TO_DISCARD ->
                request.game.grove.graftBed.has(DieSides.D20)

            GameEffect.GAIN_ANY_DIE_TO_DISCARD ->
                availableDieSizes(request).isNotEmpty()

            GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT,
            GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS ->
                opponentsWithWisps(request).isNotEmpty()

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
        effectCheck(canExecute(request)) {
            "Resource handler cannot execute effect: ${request.effect}"
        }

        when (request.effect) {
            GameEffect.GAIN_WATER_TOKEN ->
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

            GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND ->
                gainWormAndBoostWorms(request)

            GameEffect.GAIN_ONE_WISP ->
                gainOneWisp(request, executor)

            GameEffect.GAIN_D10_TO_DISCARD ->
                gainFixedDie(request, DieSides.D10)

            GameEffect.GAIN_D12_TO_DISCARD ->
                gainFixedDie(request, DieSides.D12)

            GameEffect.GAIN_D20_TO_DISCARD ->
                gainFixedDie(request, DieSides.D20)

            GameEffect.GAIN_ANY_DIE_TO_DISCARD ->
                gainChosenDie(request)

            GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT ->
                stealRandomWispFromOneOpponent(request)

            GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS ->
                stealRandomWispFromAllOpponents(request)

            GameEffect.GAIN_OR_REFRESH_GREEN_BUTTERFLY ->
                gainOrRefreshButterfly(request, Butterfly.GREEN)

            GameEffect.GAIN_OR_REFRESH_PURPLE_BUTTERFLY ->
                gainOrRefreshButterfly(request, Butterfly.PURPLE)

            GameEffect.GAIN_OR_REFRESH_RED_BUTTERFLY ->
                gainOrRefreshButterfly(request, Butterfly.RED)

            GameEffect.GAIN_OR_REFRESH_YELLOW_BUTTERFLY ->
                gainOrRefreshButterfly(request, Butterfly.YELLOW)

            else -> unsupportedGameEffect(
                "Unsupported effect reached ResourceEffectHandler: ${request.effect}"
            )
        }
    }

    private fun gainWater(
        request: GameEffectRequest
    ) {
        val token = stateNotNull(
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
        stateCheck(emptyMulch != null) {
            "Validated Mulch effect has no empty Mulch token in Grove"
        }

        val removed = if (fromDiscard) {
            request.actor.dice.removeFromDiscard(die)
        } else {
            request.actor.dice.removeFromHand(die)
        }
        stateCheck(removed != null) {
            "Validated Mulch die could not be removed from player dice: $die"
        }
        stateCheck(request.game.grove.tokens.pull(emptyMulch) != null) {
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
                    ownedCritters = request.actor.critters.all,
                    context = request.decisionContext()
                )
            )
            decisionCheck(chosen in legalChoices) {
                "RewardStrategy returned illegal Critter choice for effect: " +
                    "$chosen; legal=$legalChoices"
            }
            stateCheck(request.game.grove.critters.remove(chosen)) {
                "Chosen Critter was no longer available in Grove: $chosen"
            }
            request.actor.critters.add(chosen)
        }
    }


    private fun gainWormAndBoostWorms(
        request: GameEffectRequest
    ) {
        /*
         * The physical Critter remains WORM. Root Appreciation says each Worm
         * is worth 2 MORE this round, so repeated resolutions stack:
         *
         *   1 -> 3 -> 5 -> 7 ...
         *
         * Because the boost is Player round state rather than a Critter
         * variant, Worms gained later in the same round use the boosted value
         * automatically.
         */
        if (request.game.grove.critters.remove(Critter.WORM)) {
            request.actor.critters.add(Critter.WORM)
        }

        request.actor.critterValues.boostForRound(
            critter = Critter.WORM,
            amount = 2
        )
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
        val card = stateNotNull(request.game.grove.wispDeck.draw()) {
            "Validated Wisp gain found an empty Wisp deck"
        }

        if (card.playImmediately) {
            executor.execute(
                GameEffectRequest(
                    game = request.game,
                    actor = request.actor,
                    effect = card.effect,
                    source = GameEffectSource.Wisp(card),
                    phase = request.phase,
                    battleState = request.battleState,
                    plantEffectPath = request.plantEffectPath
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
        stateCheck(request.game.grove.graftBed.take(sides)) {
            "Validated gained die was no longer available: $sides"
        }
        request.actor.dice.addToDiscard(
            request.game.dieFactory(sides)
        )
    }

    private fun availableDieSizes(
        request: GameEffectRequest
    ): List<DieSides> =
        DieSides.entries.filter {
            request.game.grove.graftBed.has(it)
        }

    private fun gainChosenDie(
        request: GameEffectRequest
    ) {
        val legalChoices = availableDieSizes(request)
        val chosen = request.actor.decisions.effect.chooseDieSize(
            ChooseEffectDieSizeRequest(
                effect = request.effect,
                legalChoices = legalChoices,
                context = request.decisionContext()
            )
        )
        decisionCheck(chosen in legalChoices) {
            "EffectStrategy chose unavailable gained die size: " +
                "$chosen; legal=$legalChoices"
        }
        gainFixedDie(request, chosen)
    }

    private fun opponentsWithWisps(
        request: GameEffectRequest
    ): List<PlayerId> =
        request.game.players
            .filter { it !== request.actor && it.wisps.isNotEmpty }
            .map { it.id }

    private fun stealRandomWispFromOneOpponent(
        request: GameEffectRequest
    ) {
        val legalChoices = opponentsWithWisps(request)
        val chosen = request.actor.decisions.effect.choosePlayer(
            ChooseEffectPlayerRequest(
                effect = request.effect,
                legalChoices = legalChoices,
                context = request.decisionContext()
            )
        )
        decisionCheck(chosen in legalChoices) {
            "EffectStrategy chose opponent without a stealable Wisp: " +
                "$chosen; legal=$legalChoices"
        }
        stealRandomWisp(request, chosen)
    }

    private fun stealRandomWispFromAllOpponents(
        request: GameEffectRequest
    ) {
        opponentsWithWisps(request).forEach { opponentId ->
            stealRandomWisp(request, opponentId)
        }
    }

    private fun stealRandomWisp(
        request: GameEffectRequest,
        opponentId: PlayerId
    ) {
        val opponent = stateNotNull(
            request.game.players.firstOrNull { it.id == opponentId }
        ) {
            "Validated Wisp-steal opponent is not in the game: $opponentId"
        }
        val selected = stateNotNull(
            request.game.randomizer.randomOrNull(opponent.wisps.cards.cards)
        ) {
            "Validated opponent ${opponentId.value} had no Wisp to steal"
        }
        stateCheck(opponent.wisps.remove(selected)) {
            "Randomly selected Wisp disappeared from opponent " +
                "${opponentId.value}: ${selected.name}"
        }
        request.actor.wisps.add(selected)
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
            stateCheck(request.actor.butterflies.faceUp(butterfly)) {
                "Owned Butterfly could not be refreshed: $butterfly"
            }
            return
        }

        val previousOwner = request.game.players.firstOrNull { player ->
            player !== request.actor &&
                butterfly in player.butterflies.all
        }

        if (previousOwner != null) {
            stateCheck(previousOwner.butterflies.remove(butterfly)) {
                "Butterfly could not be removed from previous owner: $butterfly"
            }
        } else {
            stateCheck(request.game.grove.butterflies.remove(butterfly)) {
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
