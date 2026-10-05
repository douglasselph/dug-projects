package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.player.decision.plant.PlantEffectDecisionTrace
import dugsolutions.leaf.v35.player.decision.plant.PlantEffectSource

/**
 * Route effect-internal decisions by source.  Only Plant-sourced effects go to
 * PlantEffectPolicy; Round/Wisp effects retain the existing EffectStrategy.
 */
internal fun GameEffectRequest.effectDecisionStrategy(): EffectStrategy =
    when (val s = source) {
        is GameEffectSource.Plant -> actor.decisions.plantEffect.strategyFor(
            PlantEffectSource(
                plantId = s.card.card.name,
                effect = effect,
                trace = PlantEffectDecisionTrace { d ->
                    game.chronicle.record(
                        Moment.PlantEffectDecision(
                            playerId = actor.id,
                            plantId = d.plantId,
                            effect = d.effect,
                            decisionKind = d.decisionKind,
                            legalChoiceIds = d.legalChoiceIds,
                            selectedChoiceId = d.selectedChoiceId,
                            referenceChoiceId = d.referenceChoiceId
                        )
                    )
                }
            ),
            actor.decisions.effect
        )
        is GameEffectSource.Round,
        is GameEffectSource.Wisp -> actor.decisions.effect
    }
