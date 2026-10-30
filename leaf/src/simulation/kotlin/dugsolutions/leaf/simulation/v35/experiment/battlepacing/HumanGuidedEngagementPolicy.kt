package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleSupportPriority
import dugsolutions.leaf.v35.player.decision.battle.*
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.player.decision.wisp.ChooseWispPlayRequest
import dugsolutions.leaf.v35.player.decision.wisp.WispPlayObservation
import dugsolutions.leaf.v35.player.decision.wisp.WispPlayDecision
import dugsolutions.leaf.v35.round.domain.RoundCardType

/**
 * Exploratory B adapter: uses the existing Human Baseline Main/Step-5 policies
 * and B11/B12 Support priority, without changing C04 or production policies.
 * NOT a calibrated, equivalently skilled policy for a new action economy.
 */
class HumanGuidedEngagementPolicy(private val game: Game) : EngagementPolicy {
    private val supportPriority = BattleSupportPriority()
    private val mainTiming = EngagementMainTimingPlanner()
    // The row decision is fixed for the actor's opportunity; recompute at the
    // beginning of the next Engagement, never after each Support within a turn.
    private val rowCommitments = mutableMapOf<Pair<dugsolutions.leaf.v35.player.PlayerId, dugsolutions.leaf.v35.battle.domain.StrikeRow>, Boolean>()

    override fun choose(request: EngagementDecision): EngagementChoice {
        val player = game.players.first { it.id == request.playerId }
        val card = game.config.roundValues.cardFor(requireNotNull(game.currentRound))
        val scoredSupports = request.legalSupports.map { support ->
            support to supportPriority.score(
                context = request.context, roundCard = card,
                action = support, currentFinalMains = request.legalMains,
                legalSupports = request.legalSupports
            ).total
        }.filter { it.second > 0 }.sortedByDescending { it.second }
        // Respect the same held-Wisp choice policy as the canonical Human
        // coordinator; scoring a Wisp positively is not permission to auto-play.
        val wispCards = scoredSupports.mapNotNull { (action, _) ->
            ((action as? BattleSupportAction.Shared)?.action as? SupportAction.PlayWisp)?.card
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
        val valuableSupports = scoredSupports.filter { (action, _) ->
            val wisp = ((action as? BattleSupportAction.Shared)?.action as? SupportAction.PlayWisp)?.card
            wisp == null || wisp == allowedWisp
        }

        // Main timing is a separate Candidate B choice. The canonical Main
        // selector still evaluates the full unresolved Battle grid; Main
        // timing may defer a token so it can be used in a later Engagement.
        val commitMain = rowCommitments.getOrPut(request.playerId to request.row) {
            request.legalMains.isNotEmpty() && mainTiming.shouldUseMain(
                request.context, request.row, request.normalMainsRemaining
            )
        }
        val offeredMains = if (commitMain) request.legalMains else emptyList()

        // The first normal Main remains the v37 First Main decision even if
        // it is delayed to Middle or preceded by a Support in this turn.
        // A normal Main never ends this Candidate B Support opportunity.
        if (request.normalMainsRemaining == 2 && offeredMains.isNotEmpty()) {
            val choice = player.decisions.battle.chooseFirstMainAction(
                ChooseBattleFirstMainActionRequest(card, offeredMains, request.context)
            )
            check(choice in offeredMains)
            return EngagementChoice.Main(choice)
        }

        if (offeredMains.isNotEmpty()) {
            val legal = offeredMains.map { BattleTurnAction.FinalMain(it) } +
                valuableSupports.map { BattleTurnAction.Support(it.first) }
            val chosen = player.decisions.battle.chooseTurnAction(
                ChooseBattleTurnActionRequest(card, 1 + request.actionsAlreadyTaken, legal, request.context)
            )
            check(chosen in legal)
            return when (chosen) {
                is BattleTurnAction.FinalMain -> EngagementChoice.Main(chosen.action)
                is BattleTurnAction.Support -> EngagementChoice.Support(chosen.action)
            }
        }
        return valuableSupports.firstOrNull()?.let { EngagementChoice.Support(it.first) }
            ?: EngagementChoice.Pass
    }
}
