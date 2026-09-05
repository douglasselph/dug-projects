package dugsolutions.leaf.v35.game.di

import dugsolutions.leaf.v35.chronicle.GameChronicle
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.grove.di.GroveFactory
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.di.PlayerFactory
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.random.die.di.DieFactory
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundDeck
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.wisp.domain.WispCard

/**
 * Creates a complete isolated mutable Game graph.
 *
 * GameFactory itself may be application-wide because it retains only shared
 * catalog/factory dependencies. Every invocation creates fresh game state.
 */
class GameFactory(
    private val groveFactory: GroveFactory,
    private val roundCardManager: RoundCardManager
) {
    operator fun invoke(
        config: GameConfig,
        randomizer: Randomizer = Randomizer.create(config.seed),
        exactRoundCards: List<RoundCard>? = null,
        exactWispCards: List<WispCard>? = null
    ): Game {
        val dieFactory =
            DieFactory(randomizer)

        dieFactory.config =
            config.dieConfig

        /*
         * PlayerFactory captures die creation and therefore this Game's
         * DieFactory/Randomizer. It is intentionally constructed per Game.
         */
        val playerFactory =
            PlayerFactory { sides ->
                dieFactory(sides)
            }

        val players =
            config.playerDecisionFactories.mapIndexed { index, decisionFactory ->
                playerFactory(
                    id = PlayerId(index + 1),
                    decisions = decisionFactory.create()
                )
            }

        val grove =
            groveFactory(
                selectedPlantCards =
                    config.selectedPlantCards,
                randomizer =
                    randomizer,
                exactWispCards =
                    exactWispCards
            )

        val roundDeck =
            RoundDeck(
                roundCardManager =
                    roundCardManager,
                randomizer =
                    randomizer
            )

        if (exactRoundCards != null) {
            require(exactRoundCards.size == config.roundSetup.totalRounds) {
                "Exact Round deck size must match configured total rounds: " +
                    "exact=${exactRoundCards.size}, configured=${config.roundSetup.totalRounds}"
            }
            roundDeck.setupExact(exactRoundCards)
        } else {
            setupRoundDeck(
                roundDeck = roundDeck,
                setup = config.roundSetup
            )
        }

        return Game(
            config = config,
            grove = grove,
            players = players,
            chronicle = GameChronicle(),
            roundDeck = roundDeck,
            randomizer = randomizer,
            dieFactory = dieFactory
        )
    }

    private fun setupRoundDeck(
        roundDeck: RoundDeck,
        setup: GameRoundSetup
    ) {
        when (setup) {
            is GameRoundSetup.Ordered ->
                roundDeck.setup(
                    numBattle =
                        setup.battleRounds,
                    numCultivation =
                        setup.cultivationRounds
                )
        }
    }
}
