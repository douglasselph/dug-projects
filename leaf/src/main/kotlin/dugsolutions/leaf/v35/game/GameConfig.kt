package dugsolutions.leaf.v35.game

import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.random.die.di.DieFactory

/**
 * Creates a fresh DecisionDirector for one player each time a Game is built.
 *
 * GameConfig stores factories instead of live strategy objects so a reusable
 * simulation config cannot accidentally share mutable strategy state between
 * separate Games.
 */
fun interface PlayerDecisionFactory {
    fun create(): DecisionDirector

    companion object {
        fun mechanicalControl(): PlayerDecisionFactory =
            PlayerDecisionFactory {
                DecisionDirector.mechanicalControl()
            }

        fun humanBaseline(): PlayerDecisionFactory =
            PlayerDecisionFactory {
                DecisionDirector.humanBaseline()
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
    val seed: Long? = null,
    val dieConfig: DieFactory.Config = DieFactory.Config.RANDOM
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

    companion object {
        /** Every player receives the deterministic Mechanical Control policy. */
        fun mechanicalControl(
            selectedPlantCards: List<PlantCard>,
            numPlayers: Int,
            roundSetup: GameRoundSetup = GameRoundSetup.standard(),
            seed: Long? = null,
            dieConfig: DieFactory.Config = DieFactory.Config.RANDOM
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
                dieConfig = dieConfig
            )
        }

        /** Every player receives the canonical Human Baseline policy. */
        fun humanBaseline(
            selectedPlantCards: List<PlantCard>,
            numPlayers: Int,
            roundSetup: GameRoundSetup = GameRoundSetup.standard(),
            seed: Long? = null,
            dieConfig: DieFactory.Config = DieFactory.Config.RANDOM
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
                dieConfig = dieConfig
            )
        }

        /** Canonical baseline now means Human Baseline. */
        fun baseline(
            selectedPlantCards: List<PlantCard>,
            numPlayers: Int,
            roundSetup: GameRoundSetup = GameRoundSetup.standard(),
            seed: Long? = null,
            dieConfig: DieFactory.Config = DieFactory.Config.RANDOM
        ): GameConfig =
            humanBaseline(
                selectedPlantCards = selectedPlantCards,
                numPlayers = numPlayers,
                roundSetup = roundSetup,
                seed = seed,
                dieConfig = dieConfig
            )

        /** Backward-compatible old name for Mechanical Control. */
        @Deprecated(
            message = "Use mechanicalControl()",
            replaceWith = ReplaceWith("mechanicalControl(selectedPlantCards, numPlayers, roundSetup, seed, dieConfig)")
        )
        fun mechanicalBaseline(
            selectedPlantCards: List<PlantCard>,
            numPlayers: Int,
            roundSetup: GameRoundSetup = GameRoundSetup.standard(),
            seed: Long? = null,
            dieConfig: DieFactory.Config = DieFactory.Config.RANDOM
        ): GameConfig =
            mechanicalControl(
                selectedPlantCards = selectedPlantCards,
                numPlayers = numPlayers,
                roundSetup = roundSetup,
                seed = seed,
                dieConfig = dieConfig
            )
    }
}

/**
 * Round-deck construction policy.
 *
 * Ordered is the only implementation today. The sealed shape deliberately
 * leaves room for Advanced Mixed Rounds without turning GameConfig into a
 * collection of loosely related booleans/counts.
 */
sealed interface GameRoundSetup {
    val totalRounds: Int

    data class Ordered(
        val cultivationRounds: Int,
        val battleRounds: Int
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

        override val totalRounds: Int
            get() = cultivationRounds + battleRounds
    }

    companion object {
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
