package dugsolutions.leaf.v35.player.decision.baseline.scoring

/**
 * Semantic meaning attached to a Human Baseline candidate.
 *
 * Influencers react to these tags instead of depending on concrete strategy
 * request types. That keeps card synergies local to the card that creates the
 * synergy and prevents unrelated strategies from hard-coding card names.
 */
enum class DecisionTag {
    ACQUIRE_BEE,
    ACQUIRE_WORM,
    ACQUIRE_WATER,
    ACQUIRE_MULCH,
    SPEND_BEE,
    SPEND_WORM,
    SPEND_WATER,
    SPEND_MULCH,
    PLAY_WISP,
    REFRESH_CREATURE
}
