package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.error.InvalidDecisionException
import dugsolutions.leaf.v35.effect.DefaultGameEffectExecutor
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FirstEffectChoiceStrategy
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.game.operation.RefreshResolver
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.operation.SupportActionExecutor
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.effect.ChooseWispsToKeepRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectWispsChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WispReckoningEffectTest {

    private val effect =
        WispReckoningEffect()

    private val nested =
        GameEffectExecutor { }

    @Test
    fun eachAffectedPlayerUsesOwnDecision_opponentsKeepThree_actorKeepsFour() {
        val actorStrategy =
            KeepIndexesStrategy(
                setOf(
                    1,
                    2,
                    3,
                    4
                )
            )
        val opponentStrategy =
            KeepIndexesStrategy(
                setOf(
                    0,
                    2,
                    4
                )
            )
        val untouchedStrategy =
            KeepIndexesStrategy(
                emptySet()
            )

        val actor =
            EffectTestFixture.player(
                id = 1,
                effectStrategy =
                    actorStrategy
            )
        val opponent =
            EffectTestFixture.player(
                id = 2,
                effectStrategy =
                    opponentStrategy
            )
        val smallHand =
            EffectTestFixture.player(
                id = 3,
                effectStrategy =
                    untouchedStrategy
            )

        val game =
            EffectTestFixture.game(
                actor,
                opponent,
                smallHand
            )

        val reckoning =
            wisp(
                "Reckoning",
                GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS
            )

        actor.wisps.add(
            reckoning
        )
        listOf(
            "A",
            "B",
            "C",
            "D",
            "E"
        ).forEach {
            actor.wisps.add(
                wisp(it)
            )
        }

        listOf(
            "O1",
            "O2",
            "O3",
            "O4",
            "O5"
        ).forEach {
            opponent.wisps.add(
                wisp(it)
            )
        }

        listOf(
            "S1",
            "S2",
            "S3"
        ).forEach {
            smallHand.wisps.add(
                wisp(it)
            )
        }

        effect.execute(
            EffectTestFixture.request(
                game = game,
                actor = actor,
                effect =
                    GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS,
                source =
                    GameEffectSource.Wisp(
                        reckoning
                    )
            ),
            nested
        )

        // The currently resolving Reckoning is still present here. The normal
        // SupportActionExecutor removes it after the effect returns.
        assertEquals(
            listOf(
                "Reckoning",
                "B",
                "C",
                "D",
                "E"
            ),
            actor.wisps.cards.cards
                .map {
                    it.name
                }
        )
        assertEquals(
            listOf(
                "O1",
                "O3",
                "O5"
            ),
            opponent.wisps.cards.cards
                .map {
                    it.name
                }
        )
        assertEquals(
            listOf(
                "S1",
                "S2",
                "S3"
            ),
            smallHand.wisps.cards.cards
                .map {
                    it.name
                }
        )

        assertEquals(
            1,
            actorStrategy.calls
        )
        assertEquals(
            4,
            actorStrategy.lastLimit
        )
        assertEquals(
            actor.id,
            actorStrategy.lastPlayer
        )

        assertEquals(
            1,
            opponentStrategy.calls
        )
        assertEquals(
            3,
            opponentStrategy.lastLimit
        )
        assertEquals(
            opponent.id,
            opponentStrategy.lastPlayer
        )

        assertEquals(
            0,
            untouchedStrategy.calls
        )
    }

    @Test
    fun resolvingWispIsExcludedButAnotherIdenticalCopyCanBeKept() {
        val strategy =
            KeepIndexesStrategy(
                setOf(
                    0,
                    1,
                    2,
                    3
                )
            )
        val actor =
            EffectTestFixture.player(
                id = 1,
                effectStrategy =
                    strategy
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        val reckoning =
            wisp(
                "Reckoning",
                GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS
            )

        // WispDeck physical copies intentionally share this same immutable
        // definition object.
        actor.wisps.add(
            reckoning
        )
        actor.wisps.add(
            reckoning
        )
        listOf(
            "A",
            "B",
            "C",
            "D"
        ).forEach {
            actor.wisps.add(
                wisp(it)
            )
        }

        effect.execute(
            EffectTestFixture.request(
                game = game,
                actor = actor,
                effect =
                    GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS,
                source =
                    GameEffectSource.Wisp(
                        reckoning
                    )
            ),
            nested
        )

        assertEquals(
            1,
            strategy.offered.count {
                it.name ==
                    "Reckoning"
            }
        )
        assertEquals(
            5,
            actor.wisps.size
        )
        assertEquals(
            2,
            actor.wisps.cards.cards
                .count {
                    it.name ==
                        "Reckoning"
                }
        )
    }

    @Test
    fun invalidLaterDecisionDoesNotPartiallyTrashEarlierPlayersWisps() {
        val validOpponentStrategy =
            KeepIndexesStrategy(
                setOf(
                    0,
                    1,
                    2
                )
            )
        val invalidActorStrategy =
            KeepTooFewStrategy()

        val actor =
            EffectTestFixture.player(
                id = 1,
                effectStrategy =
                    invalidActorStrategy
            )
        val opponent =
            EffectTestFixture.player(
                id = 2,
                effectStrategy =
                    validOpponentStrategy
            )
        val game =
            EffectTestFixture.game(
                actor,
                opponent
            )

        val reckoning =
            wisp(
                "Reckoning",
                GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS
            )
        actor.wisps.add(
            reckoning
        )
        listOf(
            "A",
            "B",
            "C",
            "D",
            "E"
        ).forEach {
            actor.wisps.add(
                wisp(it)
            )
        }
        listOf(
            "O1",
            "O2",
            "O3",
            "O4",
            "O5"
        ).forEach {
            opponent.wisps.add(
                wisp(it)
            )
        }

        val actorBefore =
            actor.wisps.cards.cards
        val opponentBefore =
            opponent.wisps.cards.cards

        assertFailsWith<
            InvalidDecisionException
        > {
            effect.execute(
                EffectTestFixture.request(
                    game = game,
                    actor = actor,
                    effect =
                        GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS,
                    source =
                        GameEffectSource.Wisp(
                            reckoning
                        )
                ),
                nested
            )
        }

        assertEquals(
            actorBefore,
            actor.wisps.cards.cards
        )
        assertEquals(
            opponentBefore,
            opponent.wisps.cards.cards
        )
    }

    @Test
    fun normalSupportPlayRemovesResolvingReckoningAfterLimitEffect() {
        val actorStrategy =
            KeepIndexesStrategy(
                setOf(
                    0,
                    1,
                    2,
                    3
                )
            )
        val actor =
            EffectTestFixture.player(
                id = 1,
                effectStrategy =
                    actorStrategy
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        val reckoning =
            wisp(
                "Reckoning",
                GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS
            )
        actor.wisps.add(
            reckoning
        )
        repeat(5) {
            actor.wisps.add(
                wisp(
                    "A$it"
                )
            )
        }

        val effectExecutor =
            DefaultGameEffectExecutor()
        val supportExecutor =
            SupportActionExecutor(
                rollResolver =
                    RollResolver(
                        grove = game.grove,
                        chronicle =
                            game.chronicle
                    ),
                refreshResolver =
                    RefreshResolver(
                        game.chronicle
                    ),
                effectExecutor =
                    effectExecutor
            )

        supportExecutor
            .executeCultivation(
                game = game,
                player = actor,
                action =
                    SupportAction.PlayWisp(
                        reckoning
                    )
            )

        assertEquals(
            4,
            actor.wisps.size
        )
        assertFalse(
            actor.wisps.cards.cards
                .any {
                    it.name ==
                        "Reckoning"
                }
        )
    }

    @Test
    fun underLimitsRequiresNoDecisionsAndDoesNothing() {
        val actorStrategy =
            KeepIndexesStrategy(
                emptySet()
            )
        val opponentStrategy =
            KeepIndexesStrategy(
                emptySet()
            )
        val actor =
            EffectTestFixture.player(
                1,
                effectStrategy =
                    actorStrategy
            )
        val opponent =
            EffectTestFixture.player(
                2,
                effectStrategy =
                    opponentStrategy
            )
        val game =
            EffectTestFixture.game(
                actor,
                opponent
            )

        val reckoning =
            wisp(
                "Reckoning",
                GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS
            )
        actor.wisps.add(
            reckoning
        )
        repeat(4) {
            actor.wisps.add(
                wisp(
                    "A$it"
                )
            )
        }
        repeat(3) {
            opponent.wisps.add(
                wisp(
                    "O$it"
                )
            )
        }

        effect.execute(
            EffectTestFixture.request(
                game = game,
                actor = actor,
                effect =
                    GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS,
                source =
                    GameEffectSource.Wisp(
                        reckoning
                    )
            ),
            nested
        )

        assertEquals(
            0,
            actorStrategy.calls
        )
        assertEquals(
            0,
            opponentStrategy.calls
        )
        assertEquals(
            5,
            actor.wisps.size
        )
        assertEquals(
            3,
            opponent.wisps.size
        )
    }

    private fun wisp(
        name: String,
        effect:
            GameEffect =
            GameEffect.GAIN_ONE_VP
    ): WispCard =
        WispCard(
            quantity = 1,
            name = name,
            title = name,
            count = 1,
            effect = effect,
            lineIcons = null,
            lineIconsHeight = 0,
            vpIcon = null,
            mainBackdrop = "",
            playImmediately = false,
            battleOnly = false
        )

    private open class KeepIndexesStrategy(
        private val keep:
            Set<Int>
    ) : FirstEffectChoiceStrategy() {
        var calls = 0
        var lastLimit = -1
        var lastPlayer:
            PlayerId? = null
        var offered =
            emptyList<
                dugsolutions.leaf.v35.player.decision.effect.EffectWispChoice
            >()

        override fun chooseWispsToKeep(
            request:
            ChooseWispsToKeepRequest
        ): EffectWispsChoice {
            calls++
            lastLimit =
                request.keepLimit
            lastPlayer =
                request.playerId
            offered =
                request.legalChoices

            return EffectWispsChoice(
                request.legalChoices
                    .filter {
                        it.index in keep
                    }
            )
        }
    }

    private class KeepTooFewStrategy :
        KeepIndexesStrategy(
            emptySet()
        ) {
        override fun chooseWispsToKeep(
            request:
            ChooseWispsToKeepRequest
        ): EffectWispsChoice =
            EffectWispsChoice(
                request.legalChoices
                    .take(
                        request.keepLimit -
                            1
                    )
            )
    }
}
