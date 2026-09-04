package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.operation.UpgradeResolver
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseOptionalEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Token

/**
 * Progressive production implementation of [GameEffectExecutor].
 *
 * This first concrete slice implements:
 *
 * - all four shared Cultivation Round effects
 *   - Sunlight: RAISE_DIE_PLUS_3
 *   - Water: GAIN_WATER_TOKEN
 *   - Mulch: MULCH_DIE_FROM_HAND
 *   - Compost: UPGRADE_DIE_FROM_HAND
 * - GAIN_ONE_VP (used by ordinary Wisps)
 * - Wispquake's immediate all-player reroll effect
 *
 * Unsupported effects report [canExecute] == false rather than silently doing
 * nothing. More effect families can be added to this executor incrementally.
 */
class DefaultGameEffectExecutor(
    private val upgradeResolver: UpgradeResolver = UpgradeResolver()
) : GameEffectExecutor {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        when (request.effect) {
            GameEffect.RAISE_DIE_PLUS_3 ->
                request.actor.dice.hand.isNotEmpty()

            GameEffect.GAIN_WATER_TOKEN ->
                request.game.grove.tokens.hasWater

            GameEffect.MULCH_DIE_FROM_HAND ->
                request.actor.dice.hand.isNotEmpty() &&
                    request.game.grove.tokens.mulchTokens.any { it.sides == null }

            GameEffect.UPGRADE_DIE_FROM_HAND ->
                upgradeChoices(request).isNotEmpty()

            GameEffect.GAIN_ONE_VP ->
                true

            GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN ->
                true

            else -> false
        }

    override fun execute(
        request: GameEffectRequest
    ) {
        check(canExecute(request)) {
            "GameEffect is not currently executable: ${request.effect}"
        }

        when (request.effect) {
            GameEffect.RAISE_DIE_PLUS_3 ->
                raiseOneDie(request, 3)

            GameEffect.GAIN_WATER_TOKEN ->
                gainWater(request)

            GameEffect.MULCH_DIE_FROM_HAND ->
                mulchFromHand(request)

            GameEffect.UPGRADE_DIE_FROM_HAND ->
                upgradeFromHand(request)

            GameEffect.GAIN_ONE_VP ->
                request.actor.addVp(1)

            GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN ->
                rerollAllPlayersDiceKeepOneOwn(request)

            else -> error(
                "Unsupported GameEffect reached execution after legality check: ${request.effect}"
            )
        }

        request.game.chronicle.record(
            Moment.Marker(
                "EFFECT_RESOLVED player=${request.actor.id.value} " +
                    "effect=${request.effect} source=${sourceName(request.source)} " +
                    "phase=${request.phase}"
            )
        )
    }

    private fun raiseOneDie(
        request: GameEffectRequest,
        amount: Int
    ) {
        val choice = chooseRequiredHandDie(
            request = request,
            legalChoices = handChoices(request.actor)
        )
        resolveHandDie(request.actor, choice).adjustBy(amount)
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
        val emptyMulch = request.game.grove.tokens.mulchTokens
            .firstOrNull { it.sides == null }
        check(emptyMulch != null) {
            "Validated Mulch effect has no empty Mulch token in Grove"
        }

        val choice = chooseRequiredHandDie(
            request = request,
            legalChoices = handChoices(request.actor)
        )
        val die = resolveHandDie(request.actor, choice)
        val sides = DieSides.from(die.sides)

        check(request.actor.dice.removeFromHand(die) != null) {
            "Validated Mulch die could not be removed from Hand: $choice"
        }
        check(request.game.grove.tokens.pull(emptyMulch) != null) {
            "Validated empty Mulch token could not be removed from Grove"
        }

        /*
         * A die stored on Mulch cannot be used until a later round.
         * Keep it pending through the rest of this Build; Cultivation cleanup
         * normalizes pending Mulch into an ordinary usable Mulch token.
         */
        request.actor.tokens.add(
            Token.PENDING_MULCH(sides)
        )
    }

    private fun upgradeFromHand(
        request: GameEffectRequest
    ) {
        val legalChoices = upgradeChoices(request)
        val choice = chooseRequiredHandDie(
            request = request,
            legalChoices = legalChoices
        )
        val die = resolveHandDie(request.actor, choice)

        upgradeResolver.upgradeFromHandToDiscard(
            game = request.game,
            player = request.actor,
            die = die
        )
    }

    /** Wispquake. */
    private fun rerollAllPlayersDiceKeepOneOwn(
        request: GameEffectRequest
    ) {
        val actorChoices = handChoices(request.actor)
        val selected = request.actor.decisions.effect.chooseOptionalDie(
            ChooseOptionalEffectDieRequest(
                effect = request.effect,
                legalChoices = actorChoices
            )
        )
        check(selected == null || selected in actorChoices) {
            "EffectStrategy returned an illegal optional die choice: $selected"
        }

        val keptDie = selected?.let {
            resolveHandDie(request.actor, it)
        }

        val rollResolver = RollResolver(
            grove = request.game.grove,
            chronicle = request.game.chronicle,
            immediateWispHandler = { player, card ->
                execute(
                    GameEffectRequest(
                        game = request.game,
                        actor = player,
                        effect = card.effect,
                        source = GameEffectSource.Wisp(card),
                        phase = request.phase
                    )
                )
            }
        )

        /*
         * Snapshot membership before the first reroll. Roll Rewards may alter
         * Critters/Wisps and can recursively execute another immediate Wisp,
         * but rerolling does not move these dice out of Hand.
         */
        val diceToReroll = request.game.players.flatMap { player ->
            player.dice.hand.map { die -> player to die }
        }

        diceToReroll.forEach { (player, die) ->
            if (!(player === request.actor && die === keptDie)) {
                rollResolver.roll(player, die)
            }
        }
    }

    private fun upgradeChoices(
        request: GameEffectRequest
    ): List<EffectDieChoice> =
        handChoices(request.actor) { die ->
            upgradeResolver.canUpgradeNormalStep(
                game = request.game,
                die = die
            )
        }

    private fun chooseRequiredHandDie(
        request: GameEffectRequest,
        legalChoices: List<EffectDieChoice>
    ): EffectDieChoice {
        check(legalChoices.isNotEmpty()) {
            "No legal die targets for effect: ${request.effect}"
        }

        val chosen = request.actor.decisions.effect.chooseDie(
            ChooseEffectDieRequest(
                effect = request.effect,
                legalChoices = legalChoices
            )
        )
        check(chosen in legalChoices) {
            "EffectStrategy returned an illegal die choice: $chosen; legal=$legalChoices"
        }
        return chosen
    }

    private fun handChoices(
        player: Player,
        predicate: (Die) -> Boolean = { true }
    ): List<EffectDieChoice> =
        player.dice.hand.mapIndexedNotNull { index, die ->
            if (!predicate(die)) {
                null
            } else {
                EffectDieChoice(
                    index = index,
                    sides = die.sides,
                    value = die.value
                )
            }
        }

    private fun resolveHandDie(
        player: Player,
        choice: EffectDieChoice
    ): Die {
        val die = player.dice.hand.getOrNull(choice.index)
        check(
            die != null &&
                die.sides == choice.sides &&
                die.value == choice.value
        ) {
            "Effect die choice is no longer valid: $choice"
        }
        return die
    }

    private fun sourceName(
        source: GameEffectSource
    ): String =
        when (source) {
            is GameEffectSource.Plant -> "PLANT_${source.card.card.name}"
            is GameEffectSource.Round -> "ROUND_${source.slot}"
            is GameEffectSource.Wisp -> "WISP_${source.card.name}"
        }
}
