package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * Compares a legal Plant activation with the other Cultivation Main Actions.
 *
 * Card-local knowledge remains in [HumanBaselineCardScorerRegistry]: each Plant
 * supplies its ordinary Cultivation value and the shared card-scoring helpers
 * add the visible value the effect can realize in the current state. This
 * wrapper adds only cross-cutting Cultivation policy that should not be copied
 * into every individual Plant scorer.
 *
 * In particular:
 *
 * - Buy-threshold reasoning uses [HumanBaselinePolicy.normalPurchasingPower],
 *   so protected Critters are not silently treated as ordinary cash.
 * - A Plant whose effect unambiguously improves permanent dice-pool strength
 *   receives the same modest, capped development nudge used by other permanent
 *   dice-development actions such as Compost.
 * - Activating a Plant does not receive a generic "Plant development" bonus:
 *   activation reuses an already grafted Plant and does not increase Plant count.
 *
 * Target/branch consistency is deliberately a later checkpoint. This scorer
 * values the best visible result a card can currently realize; the Effect
 * strategy must eventually use compatible target valuation so that result is
 * actually chosen.
 */
object PlantActivationPriority {
    fun score(
        context: DecisionContext,
        card: CreatureCard,
        cardScorers: HumanBaselineCardScorerRegistry,
        policy: HumanBaselinePolicy
    ): PriorityScore {
        var score = cardScorers.forPlant(card.card).playScore(
            context = context,
            phase = CardPhase.CULTIVATION,
            cardName = card.card.name,
            normalPurchasingPower = policy.normalPurchasingPower(context)
        )

        if (permanentlyImprovesDicePool(card.card.effect)) {
            val developmentBonus = policy.cultivationDiceDevelopmentBonus(context)
            if (developmentBonus != 0) {
                score = score.adjusted(
                    developmentBonus,
                    "Behind permanent dice-development target"
                )
            }
        }

        return score
    }

    /**
     * Effects included here must improve the permanent dice pool regardless of
     * a later branch choice. Conditional effects stay out until their branch
     * selection is aligned with top-level action scoring.
     */
    private fun permanentlyImprovesDicePool(effect: GameEffect): Boolean =
        effect == GameEffect.UPGRADE_DIE_AND_USE_NOW
}
