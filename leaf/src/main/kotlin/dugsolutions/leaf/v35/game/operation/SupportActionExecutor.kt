package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.battle.BattlePlacementResolver
import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.chronicle.domain.SupportActionKind
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.decisionNotNull
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.battle.BattleDiePlacementReason
import dugsolutions.leaf.v35.player.decision.support.ButterflyRollChoice
import dugsolutions.leaf.v35.player.decision.support.ChooseButterflyRollRequest
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.Token

/**
 * Executes the resource-backed support actions shared by Cultivation and
 * Battle. Battle adds Grid-location rules around the same underlying resource
 * actions.
 */
class SupportActionExecutor(
    private val rollResolver: RollResolver,
    private val refreshResolver: RefreshResolver,
    private val effectExecutor: GameEffectExecutor,
    private val battlePlacementResolver: BattlePlacementResolver =
        BattlePlacementResolver()
) {

    fun executeCultivation(
        game: Game,
        player: Player,
        action: SupportAction
    ) {
        when (action) {
            is SupportAction.PlayWisp ->
                playWisp(
                    game = game,
                    player = player,
                    action = action,
                    phase = GameEffectPhase.CULTIVATION,
                    battleState = null
                )

            is SupportAction.UseWaterReroll ->
                useWaterReroll(game, player, action)

            SupportAction.UseWaterRefresh ->
                useWaterRefresh(game, player)

            is SupportAction.UseMulch ->
                useMulchCultivation(game, player, action)

            is SupportAction.UseWormFlip ->
                useWormFlip(game, player, action)

            is SupportAction.UseButterfly ->
                useButterfly(player, action)
        }

        recordAction(game, player, action, GameEffectPhase.CULTIVATION)
    }

    fun executeBattle(
        game: Game,
        player: Player,
        battleState: BattleState,
        action: SupportAction
    ) {
        when (action) {
            is SupportAction.PlayWisp ->
                playWisp(
                    game = game,
                    player = player,
                    action = action,
                    phase = GameEffectPhase.BATTLE,
                    battleState = battleState
                )

            is SupportAction.UseWaterReroll -> {
                val die = resolveHandDie(player, action.die)
                decisionCheck(
                    battleState.grid.locationOf(die)?.playerId == player.id,
                    context = "SupportActionExecutor"
                ) {
                    "Battle Water reroll target is not currently on player ${player.id.value}'s Grid: ${action.die}"
                }
                useWaterReroll(game, player, action)
            }

            SupportAction.UseWaterRefresh ->
                useWaterRefresh(game, player)

            is SupportAction.UseMulch ->
                useMulchBattle(game, player, battleState, action)

            is SupportAction.UseWormFlip ->
                useWormFlip(game, player, action)

            is SupportAction.UseButterfly -> {
                val die = resolveHandDie(player, action.die)
                decisionCheck(
                    battleState.grid.locationOf(die)?.playerId == player.id,
                    context = "SupportActionExecutor"
                ) {
                    "Battle Butterfly target is not currently on player ${player.id.value}'s Grid: ${action.die}"
                }
                useButterfly(player, action)
            }
        }

        recordAction(game, player, action, GameEffectPhase.BATTLE)
    }

    private fun playWisp(
        game: Game,
        player: Player,
        action: SupportAction.PlayWisp,
        phase: GameEffectPhase,
        battleState: BattleState?
    ) {
        val card = player.wisps.cards.cards.firstOrNull { it == action.card }
        decisionCheck(card != null) {
            "Wisp is not in player hand: ${action.card.name}"
        }
        decisionCheck(!card.playImmediately) {
            "Immediate-play Wisp cannot be chosen as a normal Support Action: ${card.name}"
        }
        if (phase == GameEffectPhase.CULTIVATION) {
            decisionCheck(!card.battleOnly) {
                "Battle-only Wisp cannot be played during Cultivation: ${card.name}"
            }
        }

        val request = GameEffectRequest(
            game = game,
            actor = player,
            effect = card.effect,
            source = GameEffectSource.Wisp(card),
            phase = phase,
            battleState = battleState
        )
        effectCheck(effectExecutor.canExecute(request)) {
            "Wisp effect is not executable during $phase: ${card.name}"
        }
        effectExecutor.execute(request)

        stateCheck(player.wisps.remove(card)) {
            "Resolved Wisp could not be removed from player hand: ${card.name}"
        }
    }

    private fun useWaterReroll(
        game: Game,
        player: Player,
        action: SupportAction.UseWaterReroll
    ) {
        decisionCheck(player.tokens.hasWater) {
            "Player has no Water token"
        }
        val die = resolveHandDie(player, action.die)

        stateCheck(player.tokens.pull(Token.WATER) != null) {
            "Validated Water token could not be spent"
        }
        rollResolver.roll(player, die)
        game.grove.tokens.add(Token.WATER)
    }

    private fun useWaterRefresh(
        game: Game,
        player: Player
    ) {
        decisionCheck(player.tokens.hasWater) {
            "Player has no Water token"
        }

        stateCheck(player.tokens.pull(Token.WATER) != null) {
            "Validated Water token could not be spent"
        }
        refreshResolver.refresh(player)
        game.grove.tokens.add(Token.WATER)
    }

    private fun useMulchCultivation(
        game: Game,
        player: Player,
        action: SupportAction.UseMulch
    ) {
        val die = consumeMulchAndRoll(game, player, action)
        // Cultivation Mulch needs no extra location beyond Dice Hand.
        stateCheck(player.dice.hand.any { it === die }) {
            "Rolled Mulch die was not retained in Dice Hand"
        }
    }

    private fun useMulchBattle(
        game: Game,
        player: Player,
        battleState: BattleState,
        action: SupportAction.UseMulch
    ) {
        decisionCheck(
            battlePlacementResolver.legalRows(battleState, player).isNotEmpty(),
            context = "SupportActionExecutor"
        ) {
            "Player ${player.id.value} has no Strike Square with room for Mulch die"
        }

        val die = consumeMulchAndRoll(game, player, action)
        battlePlacementResolver.placeNewHandDie(
            battleState = battleState,
            player = player,
            die = die,
            reason = BattleDiePlacementReason.MULCH
        )
    }

    private fun consumeMulchAndRoll(
        game: Game,
        player: Player,
        action: SupportAction.UseMulch
    ): Die {
        val sides = decisionNotNull(action.token.sides) {
            "Stored Mulch Support Action requires a stored die size"
        }
        decisionCheck(action.token in player.tokens.mulchTokens) {
            "Mulch token is not owned by player: ${action.token}"
        }

        stateCheck(player.tokens.pull(action.token) != null) {
            "Validated Mulch token could not be spent"
        }

        val die = game.dieFactory(sides)
        player.dice.addToHand(die)
        rollResolver.roll(player, die)
        game.grove.tokens.add(Token.MULCH())
        return die
    }

    private fun useWormFlip(
        game: Game,
        player: Player,
        action: SupportAction.UseWormFlip
    ) {
        val current = decisionNotNull(player.creature.get(action.cardId)) {
            "Worm Flip target is not grafted: ${action.cardId}"
        }
        val worm = spendableWorm(player)
        decisionCheck(worm != null) {
            "Player has no Worm available for Flip"
        }

        stateCheck(player.critters.remove(worm)) {
            "Validated Worm could not be spent"
        }
        stateCheck(player.creature.flip(current.id)) {
            "Validated Worm Flip target could not be flipped: ${current.id}"
        }
        game.grove.critters.add(Critter.WORM)
    }

    private fun useButterfly(
        player: Player,
        action: SupportAction.UseButterfly
    ) {
        decisionCheck(action.butterfly in player.butterflies.all) {
            "Butterfly is not owned by player: ${action.butterfly}"
        }
        decisionCheck(player.butterflies.isFaceUp(action.butterfly)) {
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

        stateCheck(player.butterflies.faceDown(action.butterfly)) {
            "Used Butterfly could not be flipped face down: ${action.butterfly}"
        }
    }

    private fun resolveHandDie(
        player: Player,
        choice: HandDieChoice
    ): Die {
        val die = player.dice.hand.getOrNull(choice.index)
        decisionCheck(
            die != null &&
                die.sides == choice.sides &&
                die.value == choice.value
        ) {
            "Hand die choice is no longer valid: $choice"
        }
        return die
    }

    private fun spendableWorm(player: Player): Critter? =
        Critter.WORM.takeIf {
            player.critters.count(Critter.WORM) > 0
        }

    private fun recordAction(
        game: Game,
        player: Player,
        action: SupportAction,
        phase: GameEffectPhase
    ) {
        game.chronicle.record(
            Moment.SupportAction(
                playerId = player.id,
                phase = when (phase) {
                    GameEffectPhase.CULTIVATION -> ChroniclePhase.CULTIVATION
                    GameEffectPhase.BATTLE -> ChroniclePhase.BATTLE
                },
                action = actionKind(action)
            )
        )
    }

    private fun actionKind(action: SupportAction): SupportActionKind =
        when (action) {
            is SupportAction.PlayWisp -> SupportActionKind.WISP
            is SupportAction.UseWaterReroll -> SupportActionKind.WATER_REROLL
            SupportAction.UseWaterRefresh -> SupportActionKind.WATER_REFRESH
            is SupportAction.UseMulch -> SupportActionKind.MULCH
            is SupportAction.UseWormFlip -> SupportActionKind.WORM_FLIP
            is SupportAction.UseButterfly -> SupportActionKind.BUTTERFLY
        }
}
