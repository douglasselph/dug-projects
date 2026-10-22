package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.tokens.Butterfly

/**
 * Side-effect-free, observable-state estimates for acquisitions and Plant facing.
 * Current resources and legal sources are exact; future opportunities remain bounded estimates.
 * The returned numbers are *adjustments*, not stand-alone action priorities.
 */
object PlantResourceProjection {
    private const val BUTTERFLY_READY = 10
    private const val BUTTERFLY_FUTURE = 8
    private const val BEE_FUTURE = 6

    fun butterflySource(context: DecisionContext, ownerId: PlayerId?, butterfly: Butterfly): Int? {
        if (ownerId == null) {
            if (butterfly !in context.grove.butterflies) return null
        } else {
            val owner = context.opponents.firstOrNull { it.id == ownerId } ?: return null
            if (owner.board.butterflies.none { it.butterfly == butterfly }) return null
        }
        // Gained Butterfly is refreshed as part of this effect.
        val actorGain = BUTTERFLY_READY + BUTTERFLY_FUTURE
        val denial = if (ownerId == null) 0 else {
            val opponent = context.opponents.first { it.id == ownerId }
            val ready = opponent.board.butterflies.first { it.butterfly == butterfly }.isFaceUp
            (if (ready) BUTTERFLY_READY else 0) + BUTTERFLY_FUTURE
        }
        return actorGain + denial
    }

    fun butterflyRefresh(context: DecisionContext): Int =
        context.self.board.butterflies.count { !it.isFaceUp } * BUTTERFLY_READY

    fun butterflySequence(context: DecisionContext, effect: GameEffect): Int {
        val eligible = buildList {
            if (effect == GameEffect.GAIN_OR_STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES) {
                context.grove.butterflies.forEach { add(butterflySource(context, null, it) ?: 0) }
            }
            context.opponents.forEach { opponent ->
                opponent.board.butterflies.forEach { add(butterflySource(context, opponent.id, it.butterfly) ?: 0) }
            }
        }
        return butterflyRefresh(context) + (eligible.maxOrNull() ?: 0)
    }

    fun beeSource(context: DecisionContext, ownerId: PlayerId?): Int? {
        if (ownerId == null) {
            if (context.grove.bees <= 0) return null
        } else if (context.opponents.none { it.id == ownerId && it.board.bees > 0 }) return null
        val denial = if (ownerId == null) 0 else {
            val board = context.opponents.first { it.id == ownerId }.board
            BEE_FUTURE + (if (context.battle == null) 0 else board.beeValue * 2)
        }
        return BEE_FUTURE + denial
    }

    fun beeSequence(context: DecisionContext, effect: GameEffect): Int {
        val gained = buildList {
            beeSource(context, null)?.let { add(it) }
            context.opponents.forEach { beeSource(context, it.id)?.let { value -> add(value) } }
        }.maxOrNull() ?: 0
        val existing = context.self.board.bees
        val nextValue = if (effect == GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND) 4
            else context.self.board.beeValue + 2
        val count = existing + if (gained > 0) 1 else 0
        val increase = (nextValue - context.self.board.beeValue) * existing +
            if (gained > 0) nextValue else 0
        return gained + increase.coerceAtLeast(0) * 3 + count.coerceAtMost(3)
    }

    /** Prioritize the actual highest-value spent card, rather than the number of spent cards. */
    fun bestRefresh(context: DecisionContext, registry: HumanBaselineCardScorerRegistry): Int =
        context.self.board.creature.asSequence()
            .filter { it.isFaceDown }
            .map { card -> facingValue(context, card, registry) }
            .maxOrNull() ?: 0

    fun bestReuse(context: DecisionContext, registry: HumanBaselineCardScorerRegistry): Int =
        context.self.board.creature.asSequence()
            .filter { it.isFaceDown && (it.type.name == "ROOT" || it.type.name == "VINE") }
            .map { card -> facingValue(context, card, registry) }
            .maxOrNull() ?: 0

    fun bestOpponentDenial(context: DecisionContext, registry: HumanBaselineCardScorerRegistry, rootsAndVinesOnly: Boolean): Int =
        context.opponents.asSequence().flatMap { it.board.creature.asSequence() }
            .filter { it.isFaceUp && (!rootsAndVinesOnly || it.type.name == "ROOT" || it.type.name == "VINE") }
            .map { card -> facingValue(context, card, registry) }
            .maxOrNull() ?: 0

    private fun facingValue(
        context: DecisionContext,
        card: CreatureCardView,
        registry: HumanBaselineCardScorerRegistry
    ): Int {
        val scorer = registry.forPlant(card)
        val current = if (CardPhase.from(context.phase) == CardPhase.BATTLE) scorer.battlePlayBase else scorer.cultivationPlayBase
        // A face-up Plant is an opportunity, not guaranteed future value.
        return (current / 4).coerceAtLeast(0) + (card.cost / 2).coerceAtLeast(0)
    }
}
