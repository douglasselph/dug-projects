package dugsolutions.leaf.v35.game.round.cultivation

import dugsolutions.leaf.v35.error.stateNotNull
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.MainActionKind
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.RoundEffectSlot
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.operation.SupportActionExecutor
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.context.DecisionContextFactory
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter

/** One successfully completed Main Action. */
data class CultivationActionResult(
    val playerId: PlayerId,
    val actionNumber: Int,
    val action: CultivationMainAction
)

/** One successfully completed optional Support Action. */
data class CultivationSupportActionResult(
    val playerId: PlayerId,
    val sequence: Int,
    val action: SupportAction
)

data class CultivationBuildActionsResult(
    val actions: List<CultivationActionResult>,
    val supportActions: List<CultivationSupportActionResult> = emptyList()
)

data class CultivationBuildResult(
    val openingDrawCounts: Map<PlayerId, Int>,
    val actions: List<CultivationActionResult>,
    val supportActions: List<CultivationSupportActionResult> = emptyList()
)

/**
 * Executes Cultivation's opening Draw-and-Roll and Build action loop.
 *
 * Each player must complete exactly two Main Actions, but may interleave any
 * number of currently legal Support Actions before choosing Done.
 */
class CultivationBuildCoordinator(
    private val rollResolver: RollResolver,
    private val effectExecutor: GameEffectExecutor,
    private val supportActionExecutor: SupportActionExecutor
) {

    fun execute(
        game: Game,
        roundCard: RoundCard
    ): CultivationBuildResult {
        require(roundCard.type == RoundCardType.CULTIVATION) {
            "Cultivation Build requires a Cultivation Round card: ${roundCard.type}"
        }

        val openingDrawCounts = executeOpeningDraw(game)
        val actions = executeActions(game, roundCard)

        return CultivationBuildResult(
            openingDrawCounts = openingDrawCounts,
            actions = actions.actions,
            supportActions = actions.supportActions
        )
    }

    /** Executes only Cultivation Step 2: every player's opening Draw 3. */
    fun executeOpeningDraw(game: Game): Map<PlayerId, Int> {
        val openingDrawCounts = linkedMapOf<PlayerId, Int>()
        game.players.forEach { player ->
            var count = 0
            repeat(3) {
                if (rollResolver.draw(player) != null) count++
            }
            openingDrawCounts[player.id] = count
            game.chronicle.record(
                Moment.OpeningDrawCompleted(
                    phase = ChroniclePhase.CULTIVATION,
                    playerId = player.id,
                    count = count
                )
            )
        }
        return openingDrawCounts.toMap()
    }

    /** Executes only Cultivation Step 3: Main/Support action selection. */
    fun executeActions(
        game: Game,
        roundCard: RoundCard
    ): CultivationBuildActionsResult {
        require(roundCard.type == RoundCardType.CULTIVATION) {
            "Cultivation Build requires a Cultivation Round card: ${roundCard.type}"
        }

        val mainActionResults = mutableListOf<CultivationActionResult>()
        val supportActionResults = mutableListOf<CultivationSupportActionResult>()

        game.players.forEach { player ->
            var mainActionsUsed = 0
            var supportSequence = 0

            while (true) {
                val legalChoices = legalChoices(
                    game = game,
                    player = player,
                    roundCard = roundCard,
                    mainActionsRemaining = 2 - mainActionsUsed
                )
                stateCheck(legalChoices.isNotEmpty()) {
                    "Player ${player.id.value} has no legal Cultivation action"
                }

                val chosen = player.decisions.cultivation.chooseAction(
                    ChooseCultivationActionRequest(
                        roundCard = roundCard,
                        mainActionsRemaining = 2 - mainActionsUsed,
                        legalChoices = legalChoices,
                        context = DecisionContextFactory.create(game, player)
                    )
                )
                decisionCheck(chosen in legalChoices) {
                    "CultivationStrategy returned an action that was not offered: $chosen"
                }

                when (chosen) {
                    is CultivationAction.Main -> {
                        decisionCheck(mainActionsUsed < 2) {
                            "Player ${player.id.value} has already used both Main Actions"
                        }
                        executeMainAction(
                            game = game,
                            player = player,
                            roundCard = roundCard,
                            action = chosen.action
                        )
                        mainActionsUsed++
                        mainActionResults += CultivationActionResult(
                            playerId = player.id,
                            actionNumber = mainActionsUsed,
                            action = chosen.action
                        )
                        game.chronicle.record(
                            Moment.MainAction(
                                playerId = player.id,
                                phase = ChroniclePhase.CULTIVATION,
                                action = mainActionKind(chosen.action),
                                actionNumber = mainActionsUsed
                            )
                        )
                    }

                    is CultivationAction.Support -> {
                        supportActionExecutor.executeCultivation(
                            game = game,
                            player = player,
                            action = chosen.action
                        )
                        supportSequence++
                        supportActionResults += CultivationSupportActionResult(
                            playerId = player.id,
                            sequence = supportSequence,
                            action = chosen.action
                        )
                    }

                    CultivationAction.Done -> {
                        decisionCheck(mainActionsUsed == 2) {
                            "Player ${player.id.value} cannot finish Build before using both Main Actions"
                        }
                        break
                    }
                }
            }
        }

        return CultivationBuildActionsResult(
            actions = mainActionResults.toList(),
            supportActions = supportActionResults.toList()
        )
    }

    private fun legalChoices(
        game: Game,
        player: Player,
        roundCard: RoundCard,
        mainActionsRemaining: Int
    ): List<CultivationAction> =
        buildList {
            if (mainActionsRemaining > 0) {
                mainActions(game, player, roundCard).mapTo(this) {
                    CultivationAction.Main(it)
                }
            }

            supportActions(game, player).mapTo(this) {
                CultivationAction.Support(it)
            }

            if (mainActionsRemaining == 0) {
                add(CultivationAction.Done)
            }
        }

    private fun mainActions(
        game: Game,
        player: Player,
        roundCard: RoundCard
    ): List<CultivationMainAction> =
        buildList {
            if (!player.dice.isSupplyEmpty || !player.dice.isDiscardEmpty) {
                add(CultivationMainAction.Draw)
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
                            phase = GameEffectPhase.CULTIVATION
                        )
                    )
                }
                .mapTo(this, CultivationMainAction::ActivatePlant)

            if (canExecuteRoundEffect(game, player, roundCard, RoundEffectSlot.FIRST)) {
                add(CultivationMainAction.RoundEffect1)
            }
            if (canExecuteRoundEffect(game, player, roundCard, RoundEffectSlot.SECOND)) {
                add(CultivationMainAction.RoundEffect2)
            }
        }

    private fun supportActions(
        game: Game,
        player: Player
    ): List<SupportAction> =
        buildList {
            player.wisps.cards.cards
                .filterNot { it.playImmediately || it.battleOnly }
                .filter { card ->
                    effectExecutor.canExecute(
                        GameEffectRequest(
                            game = game,
                            actor = player,
                            effect = card.effect,
                            source = GameEffectSource.Wisp(card),
                            phase = GameEffectPhase.CULTIVATION
                        )
                    )
                }
                .mapTo(this, SupportAction::PlayWisp)

            val handDice = player.dice.hand.mapIndexed { index, die ->
                HandDieChoice(
                    index = index,
                    sides = die.sides,
                    value = die.value
                )
            }

            if (player.tokens.hasWater) {
                add(SupportAction.UseWaterRefresh)
                handDice.mapTo(this, SupportAction::UseWaterReroll)
            }

            player.tokens.mulchTokens
                .filter { it.sides != null }
                .mapTo(this, SupportAction::UseMulch)

            val hasWorm =
                player.critters.count(Critter.WORM) > 0
            if (hasWorm) {
                player.creature.cards.forEach {
                    add(SupportAction.UseWormFlip(it.id))
                }
            }

            player.butterflies.all
                .filter { player.butterflies.isFaceUp(it) }
                .forEach { butterfly ->
                    handDice.forEach { die ->
                        add(
                            SupportAction.UseButterfly(
                                butterfly = butterfly,
                                die = die
                            )
                        )
                    }
                }
        }

    private fun canExecuteRoundEffect(
        game: Game,
        player: Player,
        roundCard: RoundCard,
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
                phase = GameEffectPhase.CULTIVATION
            )
        )
    }

    private fun executeMainAction(
        game: Game,
        player: Player,
        roundCard: RoundCard,
        action: CultivationMainAction
    ) {
        when (action) {
            CultivationMainAction.Draw ->
                stateNotNull(rollResolver.draw(player)) {
                    "Draw became unavailable for player ${player.id.value}"
                }

            is CultivationMainAction.ActivatePlant -> {
                val current = player.creature.get(action.card.id)
                decisionCheck(current != null && current.isFaceUp && current == action.card) {
                    "Plant activation target is no longer legal: ${action.card.id}"
                }
                val request = GameEffectRequest(
                    game = game,
                    actor = player,
                    effect = current.card.effect,
                    source = GameEffectSource.Plant(current),
                    phase = GameEffectPhase.CULTIVATION
                )
                effectCheck(effectExecutor.canExecute(request)) {
                    "Plant effect is no longer executable: ${current.card.effect}"
                }
                effectExecutor.execute(request)
                stateCheck(player.creature.faceDown(current.id)) {
                    "Activated Plant could not be flipped face down: ${current.id}"
                }
            }

            CultivationMainAction.RoundEffect1 ->
                executeRoundEffect(game, player, roundCard, RoundEffectSlot.FIRST)

            CultivationMainAction.RoundEffect2 ->
                executeRoundEffect(game, player, roundCard, RoundEffectSlot.SECOND)
        }
    }

    private fun executeRoundEffect(
        game: Game,
        player: Player,
        roundCard: RoundCard,
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
            phase = GameEffectPhase.CULTIVATION
        )
        effectCheck(effectExecutor.canExecute(request)) {
            "Round effect is no longer executable: $effect"
        }
        effectExecutor.execute(request)
    }

    private fun mainActionKind(action: CultivationMainAction): MainActionKind =
        when (action) {
            CultivationMainAction.Draw -> MainActionKind.DRAW
            is CultivationMainAction.ActivatePlant -> MainActionKind.ACTIVATE_PLANT
            CultivationMainAction.RoundEffect1 -> MainActionKind.ROUND_EFFECT_1
            CultivationMainAction.RoundEffect2 -> MainActionKind.ROUND_EFFECT_2
        }
}
