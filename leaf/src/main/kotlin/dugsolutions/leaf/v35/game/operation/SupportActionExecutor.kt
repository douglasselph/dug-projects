package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.support.ButterflyRollChoice
import dugsolutions.leaf.v35.player.decision.support.ChooseButterflyRollRequest
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.Token

/**
 * Executes the resource-backed support actions shared by round phases.
 *
 * This first implementation is cultivation-aware. Battle will reuse this
 * vocabulary while adding Battle placement/ordering rules around it.
 */
class SupportActionExecutor(
    private val rollResolver: RollResolver,
    private val refreshResolver: RefreshResolver,
    private val effectExecutor: GameEffectExecutor
) {

    fun executeCultivation(
        game: Game,
        player: Player,
        action: SupportAction
    ) {
        when (action) {
            is SupportAction.PlayWisp -> playWisp(game, player, action)
            is SupportAction.UseWaterReroll -> useWaterReroll(game, player, action)
            SupportAction.UseWaterRefresh -> useWaterRefresh(game, player)
            is SupportAction.UseMulch -> useMulch(game, player, action)
            is SupportAction.UseWormFlip -> useWormFlip(game, player, action)
            is SupportAction.UseButterfly -> useButterfly(player, action)
        }

        game.chronicle.record(
            Moment.Marker(
                "SUPPORT_ACTION player=${player.id.value} type=${actionName(action)} phase=CULTIVATION"
            )
        )
    }

    private fun playWisp(
        game: Game,
        player: Player,
        action: SupportAction.PlayWisp
    ) {
        val card = player.wisps.cards.cards.firstOrNull { it == action.card }
        check(card != null) {
            "Wisp is not in player hand: ${action.card.name}"
        }
        check(!card.playImmediately) {
            "Immediate-play Wisp cannot be chosen as a normal Support Action: ${card.name}"
        }
        check(!card.battleOnly) {
            "Battle-only Wisp cannot be played during Cultivation: ${card.name}"
        }

        val request = GameEffectRequest(
            game = game,
            actor = player,
            effect = card.effect,
            source = GameEffectSource.Wisp(card),
            phase = GameEffectPhase.CULTIVATION
        )
        check(effectExecutor.canExecute(request)) {
            "Wisp effect is not executable during Cultivation: ${card.name}"
        }
        effectExecutor.execute(request)

        check(player.wisps.remove(card)) {
            "Resolved Wisp could not be removed from player hand: ${card.name}"
        }
    }

    private fun useWaterReroll(
        game: Game,
        player: Player,
        action: SupportAction.UseWaterReroll
    ) {
        check(player.tokens.hasWater) {
            "Player has no Water token"
        }
        val die = resolveHandDie(player, action.die)

        check(player.tokens.pull(Token.WATER) != null) {
            "Validated Water token could not be spent"
        }
        rollResolver.roll(player, die)
        game.grove.tokens.add(Token.WATER)
    }

    private fun useWaterRefresh(
        game: Game,
        player: Player
    ) {
        check(player.tokens.hasWater) {
            "Player has no Water token"
        }

        check(player.tokens.pull(Token.WATER) != null) {
            "Validated Water token could not be spent"
        }
        refreshResolver.refresh(player)
        game.grove.tokens.add(Token.WATER)
    }

    private fun useMulch(
        game: Game,
        player: Player,
        action: SupportAction.UseMulch
    ) {
        val sides = checkNotNull(action.token.sides) {
            "Stored Mulch Support Action requires a stored die size"
        }
        check(action.token in player.tokens.mulchTokens) {
            "Mulch token is not owned by player: ${action.token}"
        }

        check(player.tokens.pull(action.token) != null) {
            "Validated Mulch token could not be spent"
        }

        val die = game.dieFactory(sides)
        player.dice.addToHand(die)
        rollResolver.roll(player, die)
        game.grove.tokens.add(Token.MULCH())
    }

    private fun useWormFlip(
        game: Game,
        player: Player,
        action: SupportAction.UseWormFlip
    ) {
        val current = checkNotNull(player.creature.get(action.cardId)) {
            "Worm Flip target is not grafted: ${action.cardId}"
        }
        val worm = spendableWorm(player)
        check(worm != null) {
            "Player has no Worm available for Flip"
        }

        check(player.critters.remove(worm)) {
            "Validated Worm could not be spent"
        }
        check(player.creature.flip(current.id)) {
            "Validated Worm Flip target could not be flipped: ${current.id}"
        }
        game.grove.critters.add(Critter.WORM)
    }

    private fun useButterfly(
        player: Player,
        action: SupportAction.UseButterfly
    ) {
        check(action.butterfly in player.butterflies.all) {
            "Butterfly is not owned by player: ${action.butterfly}"
        }
        check(player.butterflies.isFaceUp(action.butterfly)) {
            "Butterfly is not face up: ${action.butterfly}"
        }
        val die = resolveHandDie(player, action.die)
        val originalValue = die.value

        val rerolled = rollResolver.roll(player, die)
        val choice = player.decisions.support.chooseButterflyRoll(
            ChooseButterflyRollRequest(
                sides = die.sides,
                originalValue = originalValue,
                rerolledValue = rerolled.die.value
            )
        )

        when (choice) {
            ButterflyRollChoice.ORIGINAL -> die.adjustTo(originalValue)
            ButterflyRollChoice.REROLLED -> Unit
        }

        check(player.butterflies.faceDown(action.butterfly)) {
            "Used Butterfly could not be flipped face down: ${action.butterfly}"
        }
    }

    private fun resolveHandDie(
        player: Player,
        choice: HandDieChoice
    ): Die {
        val die = player.dice.hand.getOrNull(choice.index)
        check(
            die != null &&
                die.sides == choice.sides &&
                die.value == choice.value
        ) {
            "Hand die choice is no longer valid: $choice"
        }
        return die
    }

    private fun spendableWorm(player: Player): Critter? =
        when {
            player.critters.count(Critter.WORM) > 0 -> Critter.WORM
            player.critters.count(Critter.BOOSTED_WORM) > 0 -> Critter.BOOSTED_WORM
            else -> null
        }

    private fun actionName(action: SupportAction): String =
        when (action) {
            is SupportAction.PlayWisp -> "WISP"
            is SupportAction.UseWaterReroll -> "WATER_REROLL"
            SupportAction.UseWaterRefresh -> "WATER_REFRESH"
            is SupportAction.UseMulch -> "MULCH"
            is SupportAction.UseWormFlip -> "WORM_FLIP"
            is SupportAction.UseButterfly -> "BUTTERFLY"
        }
}
