package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DecisionContextFactory
import dugsolutions.leaf.v35.round.domain.RoundCardType

/** Immutable player-facing snapshot for one effect decision. */
internal fun GameEffectRequest.decisionContext(): DecisionContext =
    DecisionContextFactory.create(
        game = game,
        actor = actor,
        battleState = battleState,
        phaseOverride = when (phase) {
            GameEffectPhase.CULTIVATION -> RoundCardType.CULTIVATION
            GameEffectPhase.BATTLE -> RoundCardType.BATTLE
        }
    )

internal fun GameEffectRequest.decisionContextFor(player: Player): DecisionContext =
    DecisionContextFactory.create(
        game = game,
        actor = player,
        battleState = battleState,
        phaseOverride = when (phase) {
            GameEffectPhase.CULTIVATION -> RoundCardType.CULTIVATION
            GameEffectPhase.BATTLE -> RoundCardType.BATTLE
        }
    )
