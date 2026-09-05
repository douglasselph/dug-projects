package dugsolutions.leaf.v35.game.round.battle

import dugsolutions.leaf.v35.battle.BattlePlacementResolver
import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.domain.BattleMainStage
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.MainActionKind
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.chronicle.domain.SupportActionKind
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.RoundEffectSlot
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.error.stateNotNull
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.operation.SupportActionExecutor
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleDiePlacementReason
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleFirstMainActionRequest
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleTurnActionRequest
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter

enum class BattleMainActionStage {
    FIRST,
    FINAL
}

data class BattleMainActionResult(
    val playerId: PlayerId,
    val stage: BattleMainActionStage,
    val action: BattleMainAction
)

data class BattleSupportActionResult(
    val playerId: PlayerId,
    val passNumber: Int,
    val action: BattleSupportAction
)

data class BattleActionLoopResult(
    val firstMainActions: List<BattleMainActionResult>,
    val supportActions: List<BattleSupportActionResult>,
    val finalMainActions: List<BattleMainActionResult>
)

/**
 * Executes Battle Steps 4 and 5 after Rank-and-Place has produced a
 * [BattleState].
 *
 * Step 4: every player takes exactly one Main Action, left to right, with no
 * Support Actions interleaved.
 *
 * Step 5: repeated left-to-right passes. Each still-active player chooses one
 * Support Action or takes their final Main Action and leaves the action loop.
 */
class BattleActionCoordinator(
    private val rollResolver: RollResolver,
    private val effectExecutor: GameEffectExecutor,
    private val supportActionExecutor: SupportActionExecutor,
    private val placementResolver: BattlePlacementResolver =
        BattlePlacementResolver()
) {

    fun execute(
        game: Game,
        roundCard: RoundCard,
        battleState: BattleState
    ): BattleActionLoopResult {
        require(roundCard.type == RoundCardType.BATTLE) {
            "BattleActionCoordinator requires a Battle Round card: ${roundCard.type}"
        }

        validateBattleState(game, battleState)

        val firstResults = mutableListOf<BattleMainActionResult>()
        val supportResults = mutableListOf<BattleSupportActionResult>()
        val finalResults = mutableListOf<BattleMainActionResult>()

        // Step 4 — everybody's first Main Action before anybody may Support.
        battleState.playersInBattleOrder.forEach { player ->
            val legal = mainActions(game, player, roundCard, battleState)
            stateCheck(legal.isNotEmpty(), context = "BattleActionCoordinator") {
                "Player ${player.id.value} has no legal first Battle Main Action"
            }

            val chosen =
                player.decisions.battle.chooseFirstMainAction(
                    ChooseBattleFirstMainActionRequest(
                        roundCard = roundCard,
                        legalChoices = legal
                    )
                )

            decisionCheck(chosen in legal, context = "BattleActionCoordinator") {
                "BattleStrategy returned a first Main Action that was not offered: $chosen; legal=$legal"
            }

            executeMainAction(
                game = game,
                player = player,
                roundCard = roundCard,
                battleState = battleState,
                action = chosen
            )

            firstResults +=
                BattleMainActionResult(
                    playerId = player.id,
                    stage = BattleMainActionStage.FIRST,
                    action = chosen
                )

            recordMainAction(
                game = game,
                player = player,
                stage = BattleMainActionStage.FIRST,
                action = chosen
            )
        }

        // Step 5 — repeated passes, skipping players after their final Main.
        val active =
            battleState.playerIdsInBattleOrder.toMutableSet()
        var passNumber = 1

        while (active.isNotEmpty()) {
            battleState.playersInBattleOrder.forEach { player ->
                if (player.id !in active) return@forEach

                val finalMains =
                    mainActions(game, player, roundCard, battleState)
                stateCheck(finalMains.isNotEmpty(), context = "BattleActionCoordinator") {
                    "Player ${player.id.value} has no legal final Battle Main Action"
                }

                val legalChoices = buildList {
                    supportActions(game, player, battleState).mapTo(this) {
                        BattleTurnAction.Support(it)
                    }
                    finalMains.mapTo(this) {
                        BattleTurnAction.FinalMain(it)
                    }
                }

                val chosen =
                    player.decisions.battle.chooseTurnAction(
                        ChooseBattleTurnActionRequest(
                            roundCard = roundCard,
                            passNumber = passNumber,
                            legalChoices = legalChoices
                        )
                    )

                decisionCheck(
                    chosen in legalChoices,
                    context = "BattleActionCoordinator"
                ) {
                    "BattleStrategy returned a Step-5 action that was not offered: $chosen; legal=$legalChoices"
                }

                when (chosen) {
                    is BattleTurnAction.Support -> {
                        executeSupportAction(
                            game = game,
                            player = player,
                            battleState = battleState,
                            action = chosen.action
                        )
                        supportResults +=
                            BattleSupportActionResult(
                                playerId = player.id,
                                passNumber = passNumber,
                                action = chosen.action
                            )
                    }

                    is BattleTurnAction.FinalMain -> {
                        executeMainAction(
                            game = game,
                            player = player,
                            roundCard = roundCard,
                            battleState = battleState,
                            action = chosen.action
                        )
                        finalResults +=
                            BattleMainActionResult(
                                playerId = player.id,
                                stage = BattleMainActionStage.FINAL,
                                action = chosen.action
                            )
                        active.remove(player.id)
                        recordMainAction(
                            game = game,
                            player = player,
                            stage = BattleMainActionStage.FINAL,
                            action = chosen.action
                        )
                    }
                }
            }

            passNumber++
        }

        return BattleActionLoopResult(
            firstMainActions = firstResults.toList(),
            supportActions = supportResults.toList(),
            finalMainActions = finalResults.toList()
        )
    }

    private fun validateBattleState(
        game: Game,
        battleState: BattleState
    ) {
        stateCheck(
            battleState.playerIdsInBattleOrder.toSet() ==
                game.players.map { it.id }.toSet(),
            context = "BattleActionCoordinator"
        ) {
            "BattleState players do not match Game players"
        }

        battleState.playersInBattleOrder.forEach { player ->
            player.dice.hand.forEach { die ->
                val location = battleState.grid.locationOf(die)
                stateCheck(
                    location?.playerId == player.id,
                    context = "BattleActionCoordinator"
                ) {
                    "Battle action loop requires every current Hand die to be placed first; " +
                        "player=${player.id.value} die=$die location=$location"
                }
            }
        }
    }

    private fun mainActions(
        game: Game,
        player: Player,
        roundCard: RoundCard,
        battleState: BattleState
    ): List<BattleMainAction> =
        buildList {
            if (
                (!player.dice.isSupplyEmpty || !player.dice.isDiscardEmpty) &&
                placementResolver.legalRows(battleState, player).isNotEmpty()
            ) {
                add(BattleMainAction.Draw)
            }

            player.creature.cards
                .filter { it.isFaceUp }
                .filter { card ->
                    effectExecutor.canExecute(
                        GameEffectRequest(
                            game = game,
                            actor = player,
                            effect = card.card.effect,
                            source = GameEffectSource.Plant(card),
                            phase = GameEffectPhase.BATTLE,
                            battleState = battleState
                        )
                    )
                }
                .mapTo(this, BattleMainAction::ActivatePlant)

            if (
                canExecuteRoundEffect(
                    game,
                    player,
                    roundCard,
                    battleState,
                    RoundEffectSlot.FIRST
                )
            ) {
                add(BattleMainAction.RoundEffect1)
            }

            if (
                canExecuteRoundEffect(
                    game,
                    player,
                    roundCard,
                    battleState,
                    RoundEffectSlot.SECOND
                )
            ) {
                add(BattleMainAction.RoundEffect2)
            }
        }

    private fun supportActions(
        game: Game,
        player: Player,
        battleState: BattleState
    ): List<BattleSupportAction> =
        buildList {
            player.wisps.cards.cards
                .filterNot { it.playImmediately }
                .filter { card ->
                    effectExecutor.canExecute(
                        GameEffectRequest(
                            game = game,
                            actor = player,
                            effect = card.effect,
                            source = GameEffectSource.Wisp(card),
                            phase = GameEffectPhase.BATTLE,
                            battleState = battleState
                        )
                    )
                }
                .forEach {
                    add(BattleSupportAction.Shared(SupportAction.PlayWisp(it)))
                }

            val handDice =
                player.dice.hand.mapIndexedNotNull { index, die ->
                    if (
                        battleState.grid.locationOf(die)?.playerId ==
                        player.id
                    ) {
                        HandDieChoice(index, die.sides, die.value)
                    } else {
                        null
                    }
                }

            if (player.tokens.hasWater) {
                add(BattleSupportAction.Shared(SupportAction.UseWaterRefresh))
                handDice.forEach {
                    add(
                        BattleSupportAction.Shared(
                            SupportAction.UseWaterReroll(it)
                        )
                    )
                }
            }

            if (placementResolver.legalRows(battleState, player).isNotEmpty()) {
                player.tokens.mulchTokens
                    .filter { it.sides != null }
                    .forEach {
                        add(
                            BattleSupportAction.Shared(
                                SupportAction.UseMulch(it)
                            )
                        )
                    }
            }

            if (player.critters.count(Critter.WORM) > 0) {
                player.creature.cards.forEach {
                    add(
                        BattleSupportAction.Shared(
                            SupportAction.UseWormFlip(it.id)
                        )
                    )
                }
            }

            player.butterflies.all
                .filter { player.butterflies.isFaceUp(it) }
                .forEach { butterfly ->
                    handDice.forEach { die ->
                        add(
                            BattleSupportAction.Shared(
                                SupportAction.UseButterfly(
                                    butterfly = butterfly,
                                    die = die
                                )
                            )
                        )
                    }
                }

            val openRows =
                StrikeRow.entries.filter { row ->
                    !battleState.grid.isRowClosed(row) &&
                        !battleState.grid.isPlayerWithdrawn(player.id, row)
                }

            Critter.entries.forEach { critter ->
                if (player.critters.count(critter) > 0) {
                    openRows.forEach { row ->
                        add(
                            BattleSupportAction.PlaceCritter(
                                critter = critter,
                                row = row
                            )
                        )
                    }
                }
            }
        }

    private fun executeMainAction(
        game: Game,
        player: Player,
        roundCard: RoundCard,
        battleState: BattleState,
        action: BattleMainAction
    ) {
        when (action) {
            BattleMainAction.Draw -> {
                val rolled =
                    stateNotNull(
                        rollResolver.draw(player),
                        context = "BattleActionCoordinator"
                    ) {
                        "Battle Draw became unavailable for player ${player.id.value}"
                    }

                placementResolver.placeNewHandDie(
                    battleState = battleState,
                    player = player,
                    die = rolled.die,
                    reason = BattleDiePlacementReason.MAIN_DRAW
                )
            }

            is BattleMainAction.ActivatePlant -> {
                val current = player.creature.get(action.card.id)
                decisionCheck(
                    current != null &&
                        current.isFaceUp &&
                        current == action.card,
                    context = "BattleActionCoordinator"
                ) {
                    "Battle Plant activation target is no longer legal: ${action.card.id}"
                }

                val request = GameEffectRequest(
                    game = game,
                    actor = player,
                    effect = current.card.effect,
                    source = GameEffectSource.Plant(current),
                    phase = GameEffectPhase.BATTLE,
                    battleState = battleState
                )
                effectCheck(effectExecutor.canExecute(request)) {
                    "Battle Plant effect is no longer executable: ${current.card.effect}"
                }
                effectExecutor.execute(request)
                stateCheck(player.creature.faceDown(current.id)) {
                    "Activated Battle Plant could not be flipped face down: ${current.id}"
                }
            }

            BattleMainAction.RoundEffect1 ->
                executeRoundEffect(
                    game,
                    player,
                    roundCard,
                    battleState,
                    RoundEffectSlot.FIRST
                )

            BattleMainAction.RoundEffect2 ->
                executeRoundEffect(
                    game,
                    player,
                    roundCard,
                    battleState,
                    RoundEffectSlot.SECOND
                )
        }
    }

    private fun executeSupportAction(
        game: Game,
        player: Player,
        battleState: BattleState,
        action: BattleSupportAction
    ) {
        when (action) {
            is BattleSupportAction.Shared ->
                supportActionExecutor.executeBattle(
                    game = game,
                    player = player,
                    battleState = battleState,
                    action = action.action
                )

            is BattleSupportAction.PlaceCritter -> {
                battleState.grid.placeCritter(
                    player = player,
                    row = action.row,
                    critter = action.critter
                )
                game.chronicle.record(
                    Moment.SupportAction(
                        playerId = player.id,
                        phase = ChroniclePhase.BATTLE,
                        action = when (action.critter) {
                            Critter.BEE -> SupportActionKind.CRITTER_BEE
                            Critter.WORM -> SupportActionKind.CRITTER_WORM
                        },
                        row = action.row
                    )
                )
            }
        }
    }

    private fun canExecuteRoundEffect(
        game: Game,
        player: Player,
        roundCard: RoundCard,
        battleState: BattleState,
        slot: RoundEffectSlot
    ): Boolean {
        val effect = when (slot) {
            RoundEffectSlot.FIRST -> roundCard.firstEffect.effect
            RoundEffectSlot.SECOND -> roundCard.secondEffect.effect
        }
        return effectExecutor.canExecute(
            GameEffectRequest(
                game = game,
                actor = player,
                effect = effect,
                source = GameEffectSource.Round(roundCard, slot),
                phase = GameEffectPhase.BATTLE,
                battleState = battleState
            )
        )
    }

    private fun executeRoundEffect(
        game: Game,
        player: Player,
        roundCard: RoundCard,
        battleState: BattleState,
        slot: RoundEffectSlot
    ) {
        val effect = when (slot) {
            RoundEffectSlot.FIRST -> roundCard.firstEffect.effect
            RoundEffectSlot.SECOND -> roundCard.secondEffect.effect
        }
        val request = GameEffectRequest(
            game = game,
            actor = player,
            effect = effect,
            source = GameEffectSource.Round(roundCard, slot),
            phase = GameEffectPhase.BATTLE,
            battleState = battleState
        )
        effectCheck(effectExecutor.canExecute(request)) {
            "Battle Round effect is no longer executable: $effect"
        }
        effectExecutor.execute(request)
    }

    private fun recordMainAction(
        game: Game,
        player: Player,
        stage: BattleMainActionStage,
        action: BattleMainAction
    ) {
        game.chronicle.record(
            Moment.MainAction(
                playerId = player.id,
                phase = ChroniclePhase.BATTLE,
                action = mainActionKind(action),
                battleStage = when (stage) {
                    BattleMainActionStage.FIRST -> BattleMainStage.FIRST
                    BattleMainActionStage.FINAL -> BattleMainStage.FINAL
                }
            )
        )
    }

    private fun mainActionKind(action: BattleMainAction): MainActionKind =
        when (action) {
            BattleMainAction.Draw -> MainActionKind.DRAW
            is BattleMainAction.ActivatePlant -> MainActionKind.ACTIVATE_PLANT
            BattleMainAction.RoundEffect1 -> MainActionKind.ROUND_EFFECT_1
            BattleMainAction.RoundEffect2 -> MainActionKind.ROUND_EFFECT_2
        }
}
