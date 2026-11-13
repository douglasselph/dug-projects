package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.game.Game

/**
 * Catalog effects recognized but not yet implemented. A newly loaded card must
 * never be silently treated as a legacy effect or a legal no-op. Remove an
 * identity from this set only when its real handler, decisions, and tests pass.
 *
 * This is an implementation checklist, not a game rule or card-data override.
 */
object CardEffectReadiness {
    val pending: Set<GameEffect> = setOf(
        GameEffect.UPGRADE_DIE_CONDITIONAL_TWO_STEPS_AND_USE_NOW,
        GameEffect.RAISE_TWO_DICE_PLUS_1_PER_ROOT_OR_VINE,
        // NC-16/17 current immediate-use Plant effects are implemented.
    )

    /**
     * Guard at the start of a full-game run. This checks the actually chosen
     * Grove and the actual physical Wisp/Round decks (including exact-deck
     * research scenarios), rather than silently omitting unsupported options.
     */
    fun requireExecutableGame(game: Game) {
        val missing = buildList {
            game.grove.plantMarket.stacks.forEach { stack ->
                if (stack.card.effect in pending) add("Plant ${stack.card.name}: ${stack.card.effect}")
            }
            game.grove.wispDeck.cards.cards.forEach { card ->
                if (card.effect in pending) add("Wisp ${card.name}: ${card.effect}")
            }
            game.roundDeck.cards.cards.forEach { card ->
                // Also examine research Round overrides, without allowing an
                // override to disguise unsupported canonical mechanics.
                listOf(card, game.config.roundValues.cardFor(card)).forEach { effective ->
                    if (effective.firstEffect.effect in pending) {
                        add("Round ${card.name}/first: ${effective.firstEffect.effect}")
                    }
                    if (effective.secondEffect.effect in pending) {
                        add("Round ${card.name}/second: ${effective.secondEffect.effect}")
                    }
                }
            }
        }.distinct()
        check(missing.isEmpty()) {
            "Cannot simulate revised card rules: recognized effects lack implementations. " +
                "Complete the effect-mechanics tasks before evaluating these cards:\n" +
                missing.joinToString("\n")
        }
    }
}
