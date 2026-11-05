package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.battle.*
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.player.decision.wisp.ChooseWispPlayRequest
import dugsolutions.leaf.v35.player.decision.wisp.WispPlayObservation
import dugsolutions.leaf.v35.player.decision.wisp.WispPlayDecision
import dugsolutions.leaf.v35.round.domain.RoundCardType

/** Exposes one frozen timing plan for each actor/Engagement, for research traces. */
interface EngagementMainPlanSource {
    fun mainPlan(playerId: PlayerId, row: StrikeRow): EngagementMainTimingDecision?
}

/**
 * v38 production adapter: delegates actual Main choices to the existing Human Baseline.
 * Timing uses action-specific v37 Main valuations; Support continuation is
 * budget-gated through production B10-B14 logic (Task 3).
 * This preserves the existing choice policy while enforcing v38 timing.
 */
class HumanGuidedEngagementPolicy(private val game: Game) : EngagementPolicy, EngagementMainPlanSource {
    private val supportContinuation = EngagementSupportContinuation()
    private val mainTiming = EngagementMainTimingPlanner()
    // The row decision is fixed for the actor's opportunity; recompute at the
    // beginning of the next Engagement, never after each Support within a turn.
    private val rowCommitments = mutableMapOf<Pair<PlayerId, StrikeRow>, EngagementMainTimingDecision>()

    override fun mainPlan(playerId: PlayerId, row: StrikeRow): EngagementMainTimingDecision? =
        rowCommitments[playerId to row]

    override fun choose(request: EngagementDecision): EngagementChoice {
        val player = game.players.first { it.id == request.playerId }
        val card = game.config.roundValues.cardFor(requireNotNull(game.currentRound))
        // Main timing is a separate Candidate B choice. The canonical Main
        // selector still evaluates the full unresolved Battle grid; Main
        // timing may defer a token so it can be used in a later Engagement.
        // Compute once, *before* taking any action in this Engagement. Recompute
        // from the new Battle context for the next row, after its predecessor
        // has resolved and locked. No policy forecasting is allowed to roll RNG.
        val plan = if (request.normalMainsRemaining > 0 && request.legalMains.isNotEmpty()) {
            rowCommitments.getOrPut(request.playerId to request.row) {
                mainTiming.plan(request.context, request.row, request.normalMainsRemaining, card, request.legalMains)
            }
        } else rowCommitments[request.playerId to request.row]
        val offeredMains = if (plan?.spendNow == true) request.legalMains else emptyList()

        // The first normal Main retains the v37 First Main chooser. Candidate B
        // commits its *timing* once per Engagement; a normal Main does not end
        // the remaining Support opportunity.
        if (request.normalMainsRemaining == 2 && offeredMains.isNotEmpty()) {
            val choice = player.decisions.battle.chooseFirstMainAction(
                ChooseBattleFirstMainActionRequest(card, offeredMains, request.context)
            )
            check(choice in offeredMains)
            val selected = player.decisions.battleMain.chooseMainAction(
                ChooseBattleMainActionRequest(
                    legalActions = offeredMains, referenceAction = choice,
                    observation = BattleMainObservation(
                        stage = BattleMainPolicyStage.FIRST,
                        roundCardName = card.name,
                        firstRoundEffect = card.firstEffect.effect,
                        secondRoundEffect = card.secondEffect.effect,
                        context = request.context
                    )
                )
            )
            check(selected in offeredMains)
            return EngagementChoice.Main(selected)
        }

        val worthwhile = supportContinuation.worthwhile(
            context = request.context,
            roundCard = card,
            legalSupports = request.legalSupports,
            normalMainsOffered = offeredMains,
            supportSlotsRemaining = request.supportSlotsRemaining
        )

        // Reuse the existing Wisp play/hold choice. The tactical continuation
        // gate says a Wisp could be useful; it does not authorize auto-playing
        // a held Wisp against that player's policy.
        val wispCards = worthwhile.mapNotNull { support ->
            ((support as? BattleSupportAction.Shared)?.action as? SupportAction.PlayWisp)?.card
        }
        val allowedWisp = if (wispCards.isEmpty()) null else {
            val decision = player.decisions.wispPlay.chooseWisp(
                ChooseWispPlayRequest(
                    legalCards = wispCards, referenceCard = null, allowHold = true,
                    observation = WispPlayObservation(
                        phase = RoundCardType.BATTLE,
                        supportPassNumber = request.actionsAlreadyTaken + 1,
                        context = request.context
                    )
                )
            )
            (decision as? WispPlayDecision.Play)?.card
        }
        val allowedAfterWisp = if (wispCards.isEmpty()) request.legalSupports else
            request.legalSupports.filter { support ->
                val wisp = ((support as? BattleSupportAction.Shared)?.action as? SupportAction.PlayWisp)?.card
                wisp == null || wisp == allowedWisp
            }
        // Just like canonical Step 5, when the Wisp policy Holds, reassess
        // continuation without those cards. Otherwise an individually valuable
        // but declined Wisp could hide a cumulative Bee/Butterfly path.
        val admitted = if (allowedAfterWisp.size == request.legalSupports.size) worthwhile else
            supportContinuation.worthwhile(
                context = request.context,
                roundCard = card,
                legalSupports = allowedAfterWisp,
                normalMainsOffered = offeredMains,
                supportSlotsRemaining = request.supportSlotsRemaining
            )

        // Supplying only worthwhile Supports to the ACTUAL v37 turn chooser
        // preserves its scoring, influences, Wisp-specific gates, and tie RNG.
        // Supplying the Final Main only if the timing plan commits it prevents
        // v37 from prematurely ending a deferred Engagement.
        val legal = offeredMains.map { BattleTurnAction.FinalMain(it) } +
            admitted.map { BattleTurnAction.Support(it) }
        if (legal.isEmpty()) return EngagementChoice.Pass
        val chosen = player.decisions.battle.chooseTurnAction(
            ChooseBattleTurnActionRequest(card, 1 + request.actionsAlreadyTaken, legal, request.context)
        )
        check(chosen in legal) { "v37 Battle policy returned unavailable Engagement action: $chosen" }
        val policyChosen = player.decisions.battleSupport.chooseSupport(
            ChooseBattleSupportActionRequest(
                legalActions = legal,
                referenceAction = chosen,
                observation = BattleSupportObservation(
                    passNumber = 1 + request.actionsAlreadyTaken,
                    roundCardName = card.name,
                    firstRoundEffect = card.firstEffect.effect,
                    secondRoundEffect = card.secondEffect.effect,
                    context = request.context
                )
            )
        )
        check(policyChosen in legal) { "Battle Support policy returned unavailable Engagement action: $policyChosen" }
        return when (policyChosen) {
            is BattleTurnAction.FinalMain -> {
                val selected = player.decisions.battleMain.chooseMainAction(
                    ChooseBattleMainActionRequest(
                        legalActions = offeredMains,
                        referenceAction = policyChosen.action,
                        observation = BattleMainObservation(
                            stage = BattleMainPolicyStage.FINAL,
                            roundCardName = card.name,
                            firstRoundEffect = card.firstEffect.effect,
                            secondRoundEffect = card.secondEffect.effect,
                            context = request.context
                        )
                    )
                )
                check(selected in offeredMains)
                EngagementChoice.Main(selected)
            }
            is BattleTurnAction.Support -> EngagementChoice.Support(policyChosen.action)
        }
    }
}
