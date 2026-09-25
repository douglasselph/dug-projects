package dugsolutions.leaf.v35.game

import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.game.intervention.MechanicalInterventionFactory
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink
import dugsolutions.leaf.v35.random.die.di.DieFactory
import dugsolutions.leaf.v35.round.domain.RoundCardType

/**
 * Creates a fresh DecisionDirector for one player each time a Game is built.
 *
 * GameConfig stores factories instead of live strategy objects so a reusable
 * simulation config cannot accidentally share mutable strategy state between
 * separate Games.
 */
fun interface PlayerDecisionFactory {
    fun create(): DecisionDirector

    /**
     * GameFactory uses this overload so a strategy receives randomness that is
     * isolated from dice, deck shuffles, and other mechanical game events.
     * Existing deterministic/scripted factories may ignore it by relying on
     * this default implementation.
     */
    fun create(strategyRandomizer: StrategyRandomizer): DecisionDirector =
        create()

    /**
     * Optional runtime debug channel. Factories that do not produce scored
     * decisions may ignore it and inherit this default behavior.
     */
    fun create(
        strategyRandomizer: StrategyRandomizer,
        reasoningSink: DecisionReasoningSink
    ): DecisionDirector =
        create(strategyRandomizer)

    companion object {
        fun mechanicalControl(): PlayerDecisionFactory =
            PlayerDecisionFactory {
                DecisionDirector.mechanicalControl()
            }

        fun humanBaseline(): PlayerDecisionFactory =
            object : PlayerDecisionFactory {
                override fun create(): DecisionDirector =
                    DecisionDirector.humanBaseline()

                override fun create(
                    strategyRandomizer: StrategyRandomizer
                ): DecisionDirector =
                    DecisionDirector.humanBaseline(strategyRandomizer)

                override fun create(
                    strategyRandomizer: StrategyRandomizer,
                    reasoningSink: DecisionReasoningSink
                ): DecisionDirector =
                    DecisionDirector.humanBaseline(
                        strategyRandomizer = strategyRandomizer,
                        reasoningSink = reasoningSink
                    )
            }

        /** Canonical baseline means Human Baseline. */
        fun baseline(): PlayerDecisionFactory =
            humanBaseline()

        /** Backward-compatible old name for Mechanical Control. */
        @Deprecated(
            message = "Use mechanicalControl()",
            replaceWith = ReplaceWith("mechanicalControl()")
        )
        fun mechanicalBaseline(): PlayerDecisionFactory =
            mechanicalControl()
    }
}

/**
 * Immutable instructions for constructing one isolated Game.
 *
 * Mutable game state is deliberately absent. GameFactory creates fresh
 * Players, Grove, Chronicle, RoundDeck, Randomizer, and dice from this config.
 */
class GameConfig(
    selectedPlantCards: List<PlantCard>,
    playerDecisionFactories: List<PlayerDecisionFactory>,
    val roundSetup: GameRoundSetup = GameRoundSetup.standard(),
    /** Mechanical game RNG seed: dice, decks, random game effects, etc. */
    val seed: Long? = null,
    val dieConfig: DieFactory.Config = DieFactory.Config.RANDOM,
    /**
     * Base seed for strategy-only tie breaking. By default it follows [seed]
     * for whole-game reproducibility while remaining a separate RNG stream.
     * Set it independently to vary strategy ties without changing mechanics.
     */
    val strategySeed: Long? = seed,
    /**
     * When true, scored strategy choices emit their selected score explanation
     * into this game's Chronicle. Disabled by default for large simulations.
     */
    val recordDecisionReasoning: Boolean = false,
    /**
     * Human-facing Chronicle verbosity. Compact output is the default. When
     * enabled, report renderers may show line-by-line opening rolls and the
     * Game also records scored decision reasoning for those detailed reports.
     */
    val chronicleDetail: Boolean = false,
    /** Experiment-only mechanical outcome replacement; fresh policy per Game. */
    val mechanicalInterventionFactory: MechanicalInterventionFactory = MechanicalInterventionFactory.NONE
) {
    val selectedPlantCards: List<PlantCard> =
        selectedPlantCards.toList()

    val playerDecisionFactories: List<PlayerDecisionFactory> =
        playerDecisionFactories.toList()

    init {
        require(this.playerDecisionFactories.size in 2..4) {
            "Game requires 2 to 4 players: ${this.playerDecisionFactories.size}"
        }
    }

    val numPlayers: Int
        get() = playerDecisionFactories.size

    /**
     * Each player gets an independent deterministic strategy stream so one
     * player's equal-score choices do not advance another player's stream.
     */
    internal fun strategySeedForPlayer(playerIndex: Int): Long? {
        require(playerIndex in playerDecisionFactories.indices) {
            "Strategy player index out of range: $playerIndex"
        }
        return strategySeed?.let { base ->
            base xor (PLAYER_STRATEGY_SEED_STEP * (playerIndex + 1L))
        }
    }

    companion object {
        private const val PLAYER_STRATEGY_SEED_STEP: Long = 0x5DEECE66DL

        /** Every player receives the deterministic Mechanical Control policy. */
        fun mechanicalControl(
            selectedPlantCards: List<PlantCard>,
            numPlayers: Int,
            roundSetup: GameRoundSetup = GameRoundSetup.standard(),
            seed: Long? = null,
            dieConfig: DieFactory.Config = DieFactory.Config.RANDOM,
            strategySeed: Long? = seed,
            recordDecisionReasoning: Boolean = false,
            chronicleDetail: Boolean = false
        ): GameConfig {
            require(numPlayers in 2..4) {
                "Game requires 2 to 4 players: $numPlayers"
            }

            return GameConfig(
                selectedPlantCards = selectedPlantCards,
                playerDecisionFactories = List(numPlayers) {
                    PlayerDecisionFactory.mechanicalControl()
                },
                roundSetup = roundSetup,
                seed = seed,
                dieConfig = dieConfig,
                strategySeed = strategySeed,
                recordDecisionReasoning = recordDecisionReasoning,
                chronicleDetail = chronicleDetail
            )
        }

        /** Every player receives the canonical Human Baseline policy. */
        fun humanBaseline(
            selectedPlantCards: List<PlantCard>,
            numPlayers: Int,
            roundSetup: GameRoundSetup = GameRoundSetup.standard(),
            seed: Long? = null,
            dieConfig: DieFactory.Config = DieFactory.Config.RANDOM,
            strategySeed: Long? = seed,
            recordDecisionReasoning: Boolean = false,
            chronicleDetail: Boolean = false
        ): GameConfig {
            require(numPlayers in 2..4) {
                "Game requires 2 to 4 players: $numPlayers"
            }

            return GameConfig(
                selectedPlantCards = selectedPlantCards,
                playerDecisionFactories = List(numPlayers) {
                    PlayerDecisionFactory.humanBaseline()
                },
                roundSetup = roundSetup,
                seed = seed,
                dieConfig = dieConfig,
                strategySeed = strategySeed,
                recordDecisionReasoning = recordDecisionReasoning,
                chronicleDetail = chronicleDetail
            )
        }

        /** Canonical baseline now means Human Baseline. */
        fun baseline(
            selectedPlantCards: List<PlantCard>,
            numPlayers: Int,
            roundSetup: GameRoundSetup = GameRoundSetup.standard(),
            seed: Long? = null,
            dieConfig: DieFactory.Config = DieFactory.Config.RANDOM,
            strategySeed: Long? = seed,
            recordDecisionReasoning: Boolean = false,
            chronicleDetail: Boolean = false
        ): GameConfig =
            humanBaseline(
                selectedPlantCards = selectedPlantCards,
                numPlayers = numPlayers,
                roundSetup = roundSetup,
                seed = seed,
                dieConfig = dieConfig,
                strategySeed = strategySeed,
                recordDecisionReasoning = recordDecisionReasoning,
                chronicleDetail = chronicleDetail
            )

        /** Backward-compatible old name for Mechanical Control. */
        @Deprecated(
            message = "Use mechanicalControl()",
            replaceWith = ReplaceWith("mechanicalControl(selectedPlantCards, numPlayers, roundSetup, seed, dieConfig, strategySeed, recordDecisionReasoning, chronicleDetail)")
        )
        fun mechanicalBaseline(
            selectedPlantCards: List<PlantCard>,
            numPlayers: Int,
            roundSetup: GameRoundSetup = GameRoundSetup.standard(),
            seed: Long? = null,
            dieConfig: DieFactory.Config = DieFactory.Config.RANDOM,
            strategySeed: Long? = seed,
            recordDecisionReasoning: Boolean = false,
            chronicleDetail: Boolean = false
        ): GameConfig =
            mechanicalControl(
                selectedPlantCards = selectedPlantCards,
                numPlayers = numPlayers,
                roundSetup = roundSetup,
                seed = seed,
                dieConfig = dieConfig,
                strategySeed = strategySeed,
                recordDecisionReasoning = recordDecisionReasoning,
                chronicleDetail = chronicleDetail
            )
    }
}

/**
 * Round-deck construction policy.
 *
 * [Ordered] selects shuffled Cultivation cards followed by shuffled Battle cards.
 * [Patterned] selects the same shuffled physical card pools, but arranges them as
 * Cultivation blocks separated by one Battle card. For example, 3/2/2 means:
 *
 * 3 Cultivation -> Battle -> 2 Cultivation -> Battle -> 2 Cultivation -> Battle.
 */
sealed interface GameRoundSetup {
    val cultivationRounds: Int
    val battleRounds: Int
    val roundTypes: List<RoundCardType>

    val totalRounds: Int
        get() = roundTypes.size

    data class Ordered(
        override val cultivationRounds: Int,
        override val battleRounds: Int
    ) : GameRoundSetup {
        init {
            require(cultivationRounds >= 0) {
                "Cultivation round count cannot be negative: $cultivationRounds"
            }
            require(battleRounds >= 0) {
                "Battle round count cannot be negative: $battleRounds"
            }
            require(cultivationRounds + battleRounds > 0) {
                "Game must contain at least one round"
            }
        }

        override val roundTypes: List<RoundCardType> =
            List(cultivationRounds) { RoundCardType.CULTIVATION } +
                List(battleRounds) { RoundCardType.BATTLE }
    }

    /**
     * Cultivation blocks separated by exactly one Battle round after each block.
     *
     * The block list is copied so setup remains immutable even when a mutable
     * caller list is supplied.
     */
    class Patterned(
        cultivationBlocks: List<Int>
    ) : GameRoundSetup {
        val cultivationBlocks: List<Int> = cultivationBlocks.toList()

        init {
            require(this.cultivationBlocks.isNotEmpty()) {
                "Patterned game must contain at least one Cultivation block"
            }
            require(this.cultivationBlocks.all { it > 0 }) {
                "Cultivation block sizes must all be positive: ${this.cultivationBlocks}"
            }
        }

        override val cultivationRounds: Int =
            this.cultivationBlocks.sum()

        override val battleRounds: Int =
            this.cultivationBlocks.size

        override val roundTypes: List<RoundCardType> =
            buildList {
                this@Patterned.cultivationBlocks.forEach { cultivationCount ->
                    repeat(cultivationCount) {
                        add(RoundCardType.CULTIVATION)
                    }
                    add(RoundCardType.BATTLE)
                }
            }

        override fun toString(): String =
            "Patterned(${cultivationBlocks.joinToString("/")})"
    }

    companion object {
        fun patterned(vararg cultivationBlocks: Int): GameRoundSetup =
            Patterned(cultivationBlocks.toList())

        fun firstGame(): GameRoundSetup =
            Ordered(
                cultivationRounds = 6,
                battleRounds = 3
            )

        fun standard(): GameRoundSetup =
            Ordered(
                cultivationRounds = 8,
                battleRounds = 4
            )

        fun extended(): GameRoundSetup =
            Ordered(
                cultivationRounds = 9,
                battleRounds = 5
            )
    }
}

