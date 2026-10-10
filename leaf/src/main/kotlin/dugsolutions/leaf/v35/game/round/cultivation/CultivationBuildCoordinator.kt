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
import dugsolutions.leaf.v35.game.round.blockedByEmptySharedResource
import dugsolutions.leaf.v35.game.round.battlesRemaining
import dugsolutions.leaf.v35.game.round.battleIsNext
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.operation.RollInterventionContext
import dugsolutions.leaf.v35.game.intervention.MechanicalRollSource
import dugsolutions.leaf.v35.game.operation.SupportActionExecutor
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DecisionContextFactory
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationMainActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainObservation
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationSupportActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationSupportDecision
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationSupportObservation
import dugsolutions.leaf.v35.player.decision.cultivation.stableCultivationSupportId
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.player.decision.wisp.ChooseWispPlayRequest
import dugsolutions.leaf.v35.player.decision.wisp.WispPlayDecision
import dugsolutions.leaf.v35.player.decision.wisp.WispPlayObservation
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
 * Last-resort circuit breaker for a malformed strategy/executor that keeps
 * producing new observable states forever. Normal Cultivation Build is far
 * below this many decision opportunities.
 */
private const val MAX_CULTIVATION_DECISIONS_PER_PLAYER = 100

/**
 * Observable state at one Cultivation Build decision boundary.
 *
 * If the same state is reached twice for one player, another call to the strategy
 * would be asking the same question again without any observable progress.
 */
private data class CultivationDecisionState(
    val mainActionsRemaining: Int,
    val legalChoices: List<CultivationAction>,
    val context: DecisionContext
)

/**
 * Executes Cultivation's opening Draw-and-Roll and Build action loop.
 *
 * Each player must complete exactly two Main Actions, but may interleave any
 * number of currently legal Support Actions before choosing Done. After every
 * completed action the coordinator rebuilds the legal choices and player-facing
 * [dugsolutions.leaf.v35.player.decision.context.DecisionContext] before asking
 * the strategy again.
 *
 * The coordinator does not trust a strategy to eventually choose Done. It keeps
 * the observable decision states seen for each player and fails if one repeats.
 * Reaching the same Main-actions-remaining value, legal choices, and decision
 * context again means the Build loop has made no observable progress and could
 * otherwise cycle forever. A generous per-player decision-count ceiling is a
 * final circuit breaker for a malformed loop that somehow changes observable
 * state on every pass.
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
                if (
                    rollResolver.draw(
                        player = player,
                        interventionContext = RollInterventionContext(
                            roundNumber = game.roundNumber,
                            roundType = RoundCardType.CULTIVATION,
                            source = MechanicalRollSource.CULTIVATION_OPENING_DRAW
                        )
                    ) != null
                ) count++
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
            var decisionCount = 0
            val seenDecisionStates = mutableSetOf<CultivationDecisionState>()

            while (true) {
                decisionCount++
                stateCheck(
                    decisionCount <= MAX_CULTIVATION_DECISIONS_PER_PLAYER,
                    context = "CultivationBuildCoordinator"
                ) {
                    "Player ${player.id.value} exceeded " +
                        "$MAX_CULTIVATION_DECISIONS_PER_PLAYER Cultivation decisions without choosing Done; " +
                        "Build may be looping"
                }

                val mainActionsRemaining = 2 - mainActionsUsed
                val legalChoices = legalChoices(
                    game = game,
                    player = player,
                    roundCard = roundCard,
                    mainActionsRemaining = mainActionsRemaining
                )
                if (mainActionsRemaining > 0) {
                    recordRoundEffectOpportunity(game, player, roundCard, legalChoices)
                }
                // A player can legitimately run out of legal Build actions. For example,
                // Battle Doom can leave them with no dice anywhere, while their face-up
                // Plants and both Round effects require a die. In that state there is no
                // decision for the strategy to make: finish this player's Build even if
                // fewer than two Main Actions were possible.
                if (legalChoices.isEmpty()) {
                    break
                }

                val context = DecisionContextFactory.create(game, player)
                val decisionState = CultivationDecisionState(
                    mainActionsRemaining = mainActionsRemaining,
                    legalChoices = legalChoices,
                    context = context
                )
                stateCheck(
                    seenDecisionStates.add(decisionState),
                    context = "CultivationBuildCoordinator"
                ) {
                    "Player ${player.id.value} repeated the same Cultivation decision state; " +
                        "Build is not making observable progress and could loop forever"
                }

                val cultivationChoice = player.decisions.cultivation.chooseAction(
                    ChooseCultivationActionRequest(
                        roundCard = roundCard,
                        mainActionsRemaining = mainActionsRemaining,
                        legalChoices = legalChoices,
                        context = context
                    )
                )
                decisionCheck(cultivationChoice in legalChoices) {
                    "CultivationStrategy returned an action that was not offered: $cultivationChoice"
                }
                val chosen = applySupportAndMainPolicies(
                    game = game,
                    player = player,
                    roundCard = roundCard,
                    mainActionsRemaining = mainActionsRemaining,
                    legalChoices = legalChoices,
                    context = context,
                    cultivationChoice = cultivationChoice
                )
                if (mainActionsRemaining > 0) {
                    recordRoundEffectChoice(game, player, roundCard, legalChoices, chosen, cultivationChoice)
                }

                when (chosen) {
                    is CultivationAction.Main -> {
                        decisionCheck(mainActionsUsed < 2) {
                            "Player ${player.id.value} has already used both Main Actions"
                        }
                        val actionNumber = mainActionsUsed + 1
                        game.chronicle.scoped(
                            Moment.MainAction(
                                playerId = player.id,
                                phase = ChroniclePhase.CULTIVATION,
                                action = mainActionKind(chosen.action),
                                actionNumber = actionNumber,
                                decisionProbabilityPercent = chosen.decisionProbabilityPercent
                            )
                        ) {
                            executeMainAction(
                                game = game,
                                player = player,
                                roundCard = roundCard,
                                action = chosen.action
                            )
                        }
                        mainActionsUsed = actionNumber
                        mainActionResults += CultivationActionResult(
                            playerId = player.id,
                            actionNumber = mainActionsUsed,
                            action = chosen.action
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
                        val hasLegalMain = legalChoices.any { it is CultivationAction.Main }
                        decisionCheck(mainActionsUsed == 2 || !hasLegalMain) {
                            "Player ${player.id.value} cannot finish Build before using both Main Actions while a legal Main remains"
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

    private fun recordRoundEffectOpportunity(
        game: Game,
        player: Player,
        roundCard: RoundCard,
        legalChoices: List<CultivationAction>
    ) {
        val mains = legalChoices.filterIsInstance<CultivationAction.Main>().map { it.action }.toSet()
        game.chronicle.record(
            Moment.RoundEffectOpportunity(
                playerId = player.id,
                phase = ChroniclePhase.CULTIVATION,
                roundCardName = roundCard.name,
                firstEffect = roundCard.firstEffect.effect,
                secondEffect = roundCard.secondEffect.effect,
                firstExecutable = CultivationMainAction.RoundEffect1 in mains,
                secondExecutable = CultivationMainAction.RoundEffect2 in mains,
                firstBlockedBySharedResource = CultivationMainAction.RoundEffect1 !in mains &&
                    blockedByEmptySharedResource(game, roundCard.firstEffect.effect),
                secondBlockedBySharedResource = CultivationMainAction.RoundEffect2 !in mains &&
                    blockedByEmptySharedResource(game, roundCard.secondEffect.effect)
            )
        )
    }

    private fun recordRoundEffectChoice(
        game: Game,
        player: Player,
        roundCard: RoundCard,
        legalChoices: List<CultivationAction>,
        chosen: CultivationAction,
        humanBaselineChoice: CultivationAction
    ) {
        val legalMains = legalChoices.filterIsInstance<CultivationAction.Main>().map { mainActionKind(it.action) }
        val selectedMain = (chosen as? CultivationAction.Main)?.action
        val humanMain = (humanBaselineChoice as? CultivationAction.Main)?.action
        game.chronicle.record(
            Moment.RoundEffectChoice(
                playerId = player.id,
                phase = ChroniclePhase.CULTIVATION,
                roundCardName = roundCard.name,
                firstEffect = roundCard.firstEffect.effect,
                secondEffect = roundCard.secondEffect.effect,
                firstExecutable = MainActionKind.ROUND_EFFECT_1 in legalMains,
                secondExecutable = MainActionKind.ROUND_EFFECT_2 in legalMains,
                legalMainActions = legalMains,
                selectedMainAction = selectedMain?.let(::mainActionKind),
                sunlightHeld = player.tokens.sunlightCount,
                battlesRemaining = battlesRemaining(game),
                battleNext = battleIsNext(game),
                humanBaselineSelectedMainAction = humanMain?.let(::mainActionKind),
                selectedPlantCardName = (selectedMain as? CultivationMainAction.ActivatePlant)?.card?.card?.name,
                humanBaselineSelectedPlantCardName = (humanMain as? CultivationMainAction.ActivatePlant)?.card?.card?.name
            )
        )
    }

    /**
     * Routes optional Cultivation Support timing through its own policy seam.
     *
     * The existing Cultivation strategy still supplies the Human/Mechanical reference
     * choice.  Human/Mechanical support policies reproduce that choice exactly. A
     * learned support policy may instead spend another legal Support or PASS. PASS
     * proceeds to a Main action when one remains, or Done after both Mains are spent.
     */
    private fun applySupportAndMainPolicies(
        game: Game,
        player: Player,
        roundCard: RoundCard,
        mainActionsRemaining: Int,
        legalChoices: List<CultivationAction>,
        context: DecisionContext,
        cultivationChoice: CultivationAction
    ): CultivationAction {
        val rawLegalSupports = legalChoices.filterIsInstance<CultivationAction.Support>().map { it.action }
        val referenceWisp = ((cultivationChoice as? CultivationAction.Support)?.action as? SupportAction.PlayWisp)?.card
        val legalSupports = selectCultivationWispCandidate(
            game = game,
            player = player,
            raw = rawLegalSupports,
            referenceCard = referenceWisp,
            mainActionsRemaining = mainActionsRemaining,
            context = context
        )
        if (legalSupports.isEmpty()) {
            return applyMainPolicy(
                game, player, roundCard, mainActionsRemaining, legalChoices, context, cultivationChoice
            )
        }

        val rawReferenceSupport = (cultivationChoice as? CultivationAction.Support)?.action
        val referenceSupport = rawReferenceSupport?.takeIf { it in legalSupports }
        val request = ChooseCultivationSupportActionRequest(
            legalActions = legalSupports,
            referenceAction = referenceSupport,
            observation = CultivationSupportObservation(
                mainActionsRemaining = mainActionsRemaining,
                roundCardName = roundCard.name,
                firstRoundEffect = roundCard.firstEffect.effect,
                secondRoundEffect = roundCard.secondEffect.effect,
                context = context
            )
        )
        val selected = player.decisions.cultivationSupport.chooseSupport(request)
        val selectedId = when (selected) {
            CultivationSupportDecision.Pass -> null
            is CultivationSupportDecision.Use -> selected.action.stableCultivationSupportId()
        }
        game.chronicle.record(
            Moment.CultivationSupportDecision(
                playerId = player.id,
                mainActionsRemaining = mainActionsRemaining,
                legalActionIds = legalSupports.map { it.stableCultivationSupportId() },
                selectedActionId = selectedId,
                referenceActionId = referenceSupport?.stableCultivationSupportId(),
                faceDownPlants = context.self.board.creature.count { it.isFaceDown },
                spentButterflies = context.self.board.butterflies.count { !it.isFaceUp }
            )
        )

        return when (selected) {
            is CultivationSupportDecision.Use -> {
                decisionCheck(selected.action in legalSupports) {
                    "CultivationSupportPolicy returned an action that was not offered: ${selected.action}"
                }
                CultivationAction.Support(selected.action)
            }
            CultivationSupportDecision.Pass -> {
                if (cultivationChoice !is CultivationAction.Support) {
                    applyMainPolicy(
                        game, player, roundCard, mainActionsRemaining, legalChoices, context, cultivationChoice
                    )
                } else if (mainActionsRemaining == 0) {
                    decisionCheck(CultivationAction.Done in legalChoices) {
                        "Cultivation Support PASS had no legal completion action"
                    }
                    CultivationAction.Done
                } else {
                    // The reference strategy wanted Support, but the support policy declined it.
                    // Ask the same strategy for its best Main-only fallback. This extra reference
                    // call is never made for Human/Mechanical support policies, preserving their
                    // historical RNG stream and exact behavior.
                    val mainOnlyChoices = legalChoices.filterIsInstance<CultivationAction.Main>()
                    if (mainOnlyChoices.isEmpty()) {
                        // A player can have unused Main tokens but no executable Main action
                        // while optional Supports remain. PASS means decline those Supports and
                        // finish Build, matching the normal no-legal-Main early-finish rule.
                        CultivationAction.Done
                    } else {
                        val fallback = player.decisions.cultivation.chooseAction(
                            ChooseCultivationActionRequest(
                                roundCard = roundCard,
                                mainActionsRemaining = mainActionsRemaining,
                                legalChoices = mainOnlyChoices,
                                context = context
                            )
                        )
                        decisionCheck(fallback is CultivationAction.Main && fallback in mainOnlyChoices) {
                            "Cultivation strategy did not return a legal Main-only fallback: $fallback"
                        }
                        applyMainPolicy(
                            game, player, roundCard, mainActionsRemaining, mainOnlyChoices, context, fallback
                        )
                    }
                }
            }
        }
    }

    /**
     * Keeps optional Cultivation Support timing with the existing Cultivation
     * strategy while routing the high-level Main choice through its independent
     * policy seam. Human and Mechanical policies return the reference choice,
     * so this refactor is behavior-preserving until a learned policy is supplied.
     */
    private fun applyMainPolicy(
        game: Game,
        player: Player,
        roundCard: RoundCard,
        mainActionsRemaining: Int,
        legalChoices: List<CultivationAction>,
        context: DecisionContext,
        cultivationChoice: CultivationAction
    ): CultivationAction {
        val referenceMain = cultivationChoice as? CultivationAction.Main ?: return cultivationChoice
        val legalMains = legalChoices.filterIsInstance<CultivationAction.Main>().map { it.action }
        val selected = player.decisions.cultivationMain.chooseMainAction(
            ChooseCultivationMainActionRequest(
                legalActions = legalMains,
                referenceAction = referenceMain.action,
                observation = CultivationMainObservation(
                    mainActionsRemaining = mainActionsRemaining,
                    roundCardName = roundCard.name,
                    firstRoundEffect = roundCard.firstEffect.effect,
                    secondRoundEffect = roundCard.secondEffect.effect,
                    context = context
                )
            )
        )
        decisionCheck(selected in legalMains) {
            "CultivationMainPolicy returned an action that was not offered: $selected"
        }
        val effective = game.config.cultivationDecisionReplay?.observe(
            roundNumber = game.roundNumber,
            playerId = player.id,
            mainActionsRemaining = mainActionsRemaining,
            roundCard = roundCard,
            roundValues = game.config.roundValues,
            legalActions = legalMains,
            chosen = selected
        ) ?: selected
        return if (effective == referenceMain.action) {
            referenceMain
        } else {
            // Probability metadata belongs to the reference Human choice and must
            // not be transferred to a replacement policy choice.
            CultivationAction.Main(effective)
        }
    }

    private fun selectCultivationWispCandidate(
        game: Game,
        player: Player,
        raw: List<SupportAction>,
        referenceCard: dugsolutions.leaf.v35.wisp.domain.WispCard?,
        mainActionsRemaining: Int,
        context: DecisionContext
    ): List<SupportAction> {
        val wisps = raw.filterIsInstance<SupportAction.PlayWisp>()
        if (wisps.isEmpty()) return raw
        val legalCards = wisps.map { it.card }
        val decision = player.decisions.wispPlay.chooseWisp(
            ChooseWispPlayRequest(
                legalCards = legalCards,
                referenceCard = referenceCard?.takeIf { c -> wisps.any { it.card == c } },
                allowHold = true,
                observation = WispPlayObservation(
                    phase = RoundCardType.CULTIVATION,
                    mainActionsRemaining = mainActionsRemaining,
                    context = context
                )
            )
        )
        game.chronicle.record(
            Moment.WispPlayDecision(
                playerId = player.id, phase = ChroniclePhase.CULTIVATION,
                legalWispNames = legalCards.map { it.name },
                selectedWispName = (decision as? WispPlayDecision.Play)?.card?.name,
                referenceWispName = referenceCard?.name,
                mainActionsRemaining = mainActionsRemaining
            )
        )
        val withoutWisps = raw.filterNot { it is SupportAction.PlayWisp }
        return when (decision) {
            WispPlayDecision.Hold -> withoutWisps
            is WispPlayDecision.Play -> withoutWisps + SupportAction.PlayWisp(decision.card)
        }
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
                            effect = game.config.plantValues.effectFor(card.card),
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
                    effect = game.config.plantValues.effectFor(current.card),
                    source = GameEffectSource.Plant(current),
                    phase = GameEffectPhase.CULTIVATION
                )
                effectCheck(effectExecutor.canExecute(request)) {
                    "Plant effect is no longer executable: ${game.config.plantValues.effectFor(current.card)}"
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
