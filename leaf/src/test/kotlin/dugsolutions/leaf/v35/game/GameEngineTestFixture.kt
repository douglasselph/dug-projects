package dugsolutions.leaf.v35.game

import dugsolutions.leaf.v35.chronicle.GameChronicle
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.grove.Grove
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.round.RoundDeck
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispDeck
import java.nio.file.Path

internal object GameEngineTestFixture {

    fun game(
        cultivationRounds: Int = 2,
        battleRounds: Int = 2,
        seed: Long = 123L,
        players: List<Player>? = null
    ): Game {
        val gamePlayers = players ?: listOf(player(1), player(2))
        val randomizer = Randomizer.create(seed)
        val setup = GameRoundSetup.Ordered(cultivationRounds, battleRounds)
        val config = GameConfig.baseline(
            selectedPlantCards = selectedCards(),
            numPlayers = gamePlayers.size,
            roundSetup = setup,
            seed = seed
        )
        val roundDeck = RoundDeck(
            roundCardManager = roundManager(),
            randomizer = randomizer
        ).apply {
            setup(
                numBattle = battleRounds,
                numCultivation = cultivationRounds
            )
        }
        val grove = Grove(
            selectedPlantCards = config.selectedPlantCards,
            wispDeck = WispDeck(WispCardManager(), randomizer)
        )

        return Game(
            config = config,
            grove = grove,
            players = gamePlayers,
            chronicle = GameChronicle(),
            roundDeck = roundDeck,
            randomizer = randomizer
        )
    }

    private fun roundManager(): RoundCardManager {
        val registry = RoundCardRegistry(GameEffectConverter())
        registry.loadFromCsv(
            Path.of("data", "v35", CardDataFiles.ROUND_CARD_LIST).toString()
        )
        return RoundCardManager().apply { loadCards(registry) }
    }

    private fun player(id: Int): Player =
        Player(PlayerId(id), DecisionDirector.baseline())

    private fun selectedCards(): List<PlantCard> =
        listOf(
            card("Root_1", PlantType.ROOT),
            card("Root_2", PlantType.ROOT),
            card("Root_3", PlantType.ROOT),
            card("Vine_1", PlantType.VINE),
            card("Vine_2", PlantType.VINE),
            card("Vine_3", PlantType.VINE),
            card("Flower_1", PlantType.FLOWER),
            card("Flower_2", PlantType.FLOWER),
            card("Flower_3", PlantType.FLOWER)
        )

    private fun card(name: String, type: PlantType): PlantCard =
        PlantCard(
            quantity = 4,
            name = name,
            title = name,
            type = type,
            cost = 5,
            lineIcon = null,
            vpIcon = "",
            typeIcon = "",
            fgColor = "",
            textColor = "",
            fullImage = "",
            backgroundImage = "",
            cardBackgroundImage = "",
            effect = GameEffect.UNKNOWN
        )
}
