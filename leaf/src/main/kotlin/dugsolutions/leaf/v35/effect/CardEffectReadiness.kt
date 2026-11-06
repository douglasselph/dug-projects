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
        GameEffect.GAIN_WORM_BOOST_AND_FREE_BATTLE_SUPPORT,
        GameEffect.RAISE_ONE_DIE_PLUS_1_PER_GRAFTED_FLOWER,
        GameEffect.RAISE_TWO_DICE_PLUS_1_PER_ROOT_OR_VINE,
        GameEffect.RAISE_ALL_D8S_PLUS_2,
        GameEffect.GAIN_OR_STEAL_BUTTERFLY_REFRESH_AND_FREE_BATTLE_SUPPORT,
        GameEffect.RAISE_ALL_DICE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER,
        GameEffect.GAIN_TWO_SUNLIGHT_TOKENS,
        GameEffect.GAIN_OR_STEAL_BEE_BOOST_AND_FREE_BATTLE_SUPPORT,
        GameEffect.FLIP_OWN_DIE_AND_OPPONENT_DIE_IN_BATTLE,
        GameEffect.GAIN_D4_USE_NOW_THEN_CYCLE_D4S,
        GameEffect.USE_DIE_FROM_DISCARD_NOW,
        GameEffect.CHOOSE_PLAYER_WITH_WISPS_TRASH_ONE,
        GameEffect.EACH_PLAYER_WITH_WISPS_TRASH_ONE,
        GameEffect.GAIN_UP_TO_D12_TO_DISCARD,
        GameEffect.GAIN_UP_TO_D8_AND_USE_NOW
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
