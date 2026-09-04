package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.error.InvalidDecisionException
import dugsolutions.leaf.v35.chronicle.GameChronicle
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.grove.Grove
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.reward.ChooseCritterRequest
import dugsolutions.leaf.v35.player.decision.reward.RewardStrategy
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import dugsolutions.leaf.v35.wisp.WispDeck
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RollResolverTest {

    private lateinit var grove: Grove
    private lateinit var chronicle: GameChronicle
    private lateinit var resolver: RollResolver

    @BeforeEach
    fun setup() {
        val registry =
            WispCardRegistry(
                GameEffectConverter()
            )

        registry.loadFromCsv(
            dataPath(CardDataFiles.WISP_LIST)
        )

        val manager =
            WispCardManager().apply {
                loadCards(registry)
            }

        grove =
            Grove(
                selectedPlantCards =
                    selectedCards(),
                wispDeck =
                    WispDeck(
                        wispCardManager = manager,
                        randomizer =
                            IdentityRandomizer()
                    )
            )

        chronicle =
            GameChronicle()

        resolver =
            RollResolver(
                grove = grove,
                chronicle = chronicle
            )
    }

    @Test
    fun draw_whenDieRollsOne_gainsCritterImmediately() {
        val player =
            playerWithSupply(
                FixedRollDie(
                    sides = 4,
                    rollValue = 1
                )
            )

        val result =
            resolver.draw(player)

        assertEquals(
            1,
            result!!.die.value
        )
        assertEquals(
            RollRewardResult.CritterGained(
                Critter.BEE
            ),
            result.reward
        )

        assertEquals(
            listOf(Critter.BEE),
            player.critters.all
        )
        assertEquals(
            8,
            grove.critters.count(
                Critter.BEE
            )
        )
        assertEquals(
            9,
            grove.critters.count(
                Critter.WORM
            )
        )

        assertEquals(
            1,
            player.dice.handSize
        )
        assertEquals(
            0,
            player.dice.supplySize
        )
    }

    @Test
    fun draw_whenOnlyWormAvailable_gainsWorm() {
        repeat(9) {
            assertTrue(
                grove.critters.remove(
                    Critter.BEE
                )
            )
        }

        val player =
            playerWithSupply(
                FixedRollDie(
                    sides = 6,
                    rollValue = 1
                )
            )

        val result =
            resolver.draw(player)

        assertEquals(
            RollRewardResult.CritterGained(
                Critter.WORM
            ),
            result!!.reward
        )
        assertEquals(
            listOf(Critter.WORM),
            player.critters.all
        )
        assertEquals(
            8,
            grove.critters.count(
                Critter.WORM
            )
        )
    }

    @Test
    fun draw_whenNoCritterAvailable_keepsDieButGainsNothing() {
        repeat(9) {
            grove.critters.remove(
                Critter.BEE
            )
            grove.critters.remove(
                Critter.WORM
            )
        }

        val player =
            playerWithSupply(
                FixedRollDie(
                    sides = 4,
                    rollValue = 1
                )
            )

        val result =
            resolver.draw(player)

        assertEquals(
            RollRewardResult.CritterUnavailable,
            result!!.reward
        )
        assertTrue(
            player.critters.isEmpty
        )
        assertEquals(
            1,
            player.dice.handSize
        )
    }

    @Test
    fun draw_whenDieRollsTwo_drawsWispIntoPlayerHand() {
        val player =
            playerWithSupply(
                FixedRollDie(
                    sides = 4,
                    rollValue = 2
                )
            )

        val before =
            grove.wispDeck.remaining

        val result =
            resolver.draw(player)

        assertTrue(
            result!!.reward is
                RollRewardResult.WispGained
        )
        assertEquals(
            1,
            player.wisps.size
        )
        assertEquals(
            before - 1,
            grove.wispDeck.remaining
        )

        val gained =
            (result.reward as
                RollRewardResult.WispGained)
                .card

        assertEquals(
            gained,
            player.wisps.cards.cards.single()
        )
    }

    @Test
    fun draw_whenRollIsThreeOrHigher_hasNoReward() {
        val player =
            playerWithSupply(
                FixedRollDie(
                    sides = 6,
                    rollValue = 5
                )
            )

        val result =
            resolver.draw(player)

        assertEquals(
            RollRewardResult.None,
            result!!.reward
        )
        assertTrue(
            player.critters.isEmpty
        )
        assertTrue(
            player.wisps.isEmpty
        )
    }

    @Test
    fun draw_whenPlayerHasNoDice_returnsNullAndRecordsNothing() {
        val player =
            Player(
                id = PlayerId(1),
                decisions =
                    DecisionDirector.baseline()
            )

        val result =
            resolver.draw(player)

        assertNull(result)
        assertTrue(
            chronicle.entries.isEmpty()
        )
    }

    @Test
    fun roll_rerollsLiveDieWithoutMovingItAndResolvesReward() {
        val die =
            FixedRollDie(
                sides = 8,
                rollValue = 1
            )

        val player =
            Player(
                id = PlayerId(1),
                decisions =
                    DecisionDirector.baseline(),
                dice =
                    PlayerDice(
                        hand = listOf(die)
                    )
            )

        val result =
            resolver.roll(
                player = player,
                die = die
            )

        assertTrue(
            result.die === die
        )
        assertEquals(
            listOf(die),
            player.dice.hand
        )
        assertEquals(
            RollRewardResult.CritterGained(
                Critter.BEE
            ),
            result.reward
        )
    }

    @Test
    fun roll_withIgnoreRewards_doesNotGainCritterOnOne() {
        val die =
            FixedRollDie(
                sides = 6,
                rollValue = 1
            )

        val player =
            Player(
                id = PlayerId(1),
                decisions =
                    DecisionDirector.baseline(),
                dice =
                    PlayerDice(
                        hand = listOf(die)
                    )
            )

        val result =
            resolver.roll(
                player = player,
                die = die,
                rewardPolicy =
                    RollRewardPolicy.IGNORE
            )

        assertEquals(
            RollRewardResult.Ignored,
            result.reward
        )
        assertTrue(
            player.critters.isEmpty
        )
        assertEquals(
            9,
            grove.critters.count(
                Critter.BEE
            )
        )
    }

    @Test
    fun roll_withIgnoreRewards_doesNotGainWispOnTwo() {
        val die =
            FixedRollDie(
                sides = 6,
                rollValue = 2
            )

        val player =
            Player(
                id = PlayerId(1),
                decisions =
                    DecisionDirector.baseline(),
                dice =
                    PlayerDice(
                        hand = listOf(die)
                    )
            )

        val before =
            grove.wispDeck.remaining

        val result =
            resolver.roll(
                player = player,
                die = die,
                rewardPolicy =
                    RollRewardPolicy.IGNORE
            )

        assertEquals(
            RollRewardResult.Ignored,
            result.reward
        )
        assertTrue(
            player.wisps.isEmpty
        )
        assertEquals(
            before,
            grove.wispDeck.remaining
        )
    }

    @Test
    fun invalidRewardStrategyChoice_isRejectedBeforeMutation() {
        repeat(9) {
            grove.critters.remove(
                Critter.BEE
            )
        }

        val invalidReward =
            object : RewardStrategy {
                override fun chooseCritter(
                    request:
                    ChooseCritterRequest
                ): Critter =
                    Critter.BEE
            }

        val player =
            Player(
                id = PlayerId(1),
                decisions =
                    DecisionDirector.baseline()
                        .copy(
                            reward =
                                invalidReward
                        ),
                dice =
                    PlayerDice(
                        supply =
                            listOf(
                                FixedRollDie(
                                    sides = 4,
                                    rollValue = 1
                                )
                            )
                    )
            )

        assertFailsWith<
            InvalidDecisionException
        > {
            resolver.draw(player)
        }

        assertTrue(
            player.critters.isEmpty
        )
        assertEquals(
            9,
            grove.critters.count(
                Critter.WORM
            )
        )
    }

    @Test
    fun roll_recordsRollAndActualRewardInChronicle() {
        val player =
            playerWithSupply(
                FixedRollDie(
                    sides = 4,
                    rollValue = 1
                )
            )

        resolver.draw(player)

        val messages =
            chronicle.entries
                .filterIsInstance<
                    GameEntry.Marker
                >()
                .map {
                    it.message
                }

        assertEquals(
            2,
            messages.size
        )
        assertTrue(
            messages[0].contains(
                "ROLL player=1"
            )
        )
        assertTrue(
            messages[1].contains(
                "ROLL_REWARD player=1"
            )
        )
        assertTrue(
            messages[1].contains(
                "CRITTER_BEE"
            )
        )
    }

    @Test
    fun rewardStrategy_receivesImmutableOwnedCritterSnapshot() {
        val observedOwned =
            mutableListOf<Critter>()

        val observingReward =
            object : RewardStrategy {
                override fun chooseCritter(
                    request:
                    ChooseCritterRequest
                ): Critter {
                    observedOwned.addAll(
                        request.ownedCritters
                    )
                    return request
                        .legalChoices
                        .first()
                }
            }

        val player =
            Player(
                id = PlayerId(1),
                decisions =
                    DecisionDirector.baseline()
                        .copy(
                            reward =
                                observingReward
                        ),
                dice =
                    PlayerDice(
                        supply =
                            listOf(
                                FixedRollDie(
                                    sides = 4,
                                    rollValue = 1
                                )
                            )
                    )
            )

        player.critters.add(
            Critter.WORM
        )

        resolver.draw(player)

        assertEquals(
            listOf(Critter.WORM),
            observedOwned
        )
    }

    @Test
    fun draw_whenWispIsImmediate_executesHandlerInsteadOfAddingToWispHand() {
        val immediate = WispCard(
            quantity = 1,
            name = "Wisp_Quake",
            title = "Wispquake",
            count = 1,
            effect = GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN,
            lineIcons = null,
            lineIconsHeight = 0,
            vpIcon = null,
            mainBackdrop = "",
            playImmediately = true
        )
        val manager = WispCardManager().apply {
            loadCards(listOf(immediate))
        }
        val localGrove = Grove(
            selectedPlantCards = selectedCards(),
            wispDeck = WispDeck(manager, IdentityRandomizer())
        )
        val localChronicle = GameChronicle()
        var handled: WispCard? = null
        val localResolver = RollResolver(
            grove = localGrove,
            chronicle = localChronicle,
            immediateWispHandler = { _, card -> handled = card }
        )
        val player = playerWithSupply(FixedRollDie(4, 2))

        val result = checkNotNull(localResolver.draw(player))

        assertEquals(immediate, handled)
        assertTrue(result.reward is RollRewardResult.WispPlayedImmediately)
        assertTrue(player.wisps.isEmpty)
    }

    private fun playerWithSupply(
        vararg dice: Die
    ): Player =
        Player(
            id = PlayerId(1),
            decisions =
                DecisionDirector.baseline(),
            dice =
                PlayerDice(
                    supply =
                        dice.toList()
                )
        )

    private fun selectedCards(): List<PlantCard> =
        listOf(
            card("Root_1", PlantType.ROOT, 5),
            card("Root_2", PlantType.ROOT, 7),
            card("Root_3", PlantType.ROOT, 9),
            card("Vine_1", PlantType.VINE, 7),
            card("Vine_2", PlantType.VINE, 9),
            card("Vine_3", PlantType.VINE, 11),
            card("Flower_1", PlantType.FLOWER, 11),
            card("Flower_2", PlantType.FLOWER, 14),
            card("Flower_3", PlantType.FLOWER, 17)
        )

    private fun card(
        name: String,
        type: PlantType,
        cost: Int
    ): PlantCard =
        PlantCard(
            quantity = 4,
            name = name,
            title = name,
            type = type,
            cost = cost,
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

    private fun dataPath(
        fileName: String
    ): String =
        Path.of(
            "data",
            "v35",
            fileName
        ).toString()

    private class FixedRollDie(
        sides: Int,
        private val rollValue: Int
    ) : Die(sides) {
        override fun roll(): Die {
            adjustTo(rollValue)
            return this
        }
    }

    private class IdentityRandomizer :
        Randomizer {

        override fun nextBoolean(): Boolean =
            false

        override fun nextInt(
            from: Int,
            until: Int
        ): Int =
            from

        override fun nextInt(
            until: Int
        ): Int =
            0

        override fun <T> randomOrNull(
            list: List<T>
        ): T? =
            list.firstOrNull()

        override fun <T> shuffled(
            list: List<T>
        ): List<T> =
            list
    }
}
