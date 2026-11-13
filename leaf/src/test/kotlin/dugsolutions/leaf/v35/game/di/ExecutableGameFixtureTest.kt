package dugsolutions.leaf.v35.game.di

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.CardEffectReadiness
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExecutableGameFixtureTest {
    @Test
    fun fixtureUsesNineLegalPlantsAndOnlyExecutablePhysicalCards() {
        val plants = PlantCardRegistry().apply {
            loadFromCsv(
                CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST),
                CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST)
            )
        }
        val wisps = WispCardRegistry(GameEffectConverter()).apply {
            loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST))
        }
        val rounds = RoundCardRegistry(GameEffectConverter()).apply {
            loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))
        }

        val selected = ExecutableGameFixture.plants(plants)
        assertEquals(9, selected.size)
        assertEquals(9, selected.map { it.type to it.cost }.toSet().size)
        assertTrue(selected.none { it.effect in CardEffectReadiness.pending })

        val physicalWisps = ExecutableGameFixture.wisps(wisps)
        assertTrue(physicalWisps.isNotEmpty())
        assertTrue(physicalWisps.none { it.effect in CardEffectReadiness.pending })
        assertTrue(physicalWisps.none { it.name == "Wisp_Award_VP" || it.name == "Wisp_Award_VP2" })
        physicalWisps.groupingBy { it.name }.eachCount().forEach { (id, count) ->
            assertTrue(count <= requireNotNull(wisps.getCard(id)).quantity)
        }

        val setup = GameRoundSetup.standard()
        val physicalRounds = ExecutableGameFixture.rounds(rounds, setup)
        assertEquals(setup.roundTypes, physicalRounds.map { it.type })
        assertEquals(10, physicalRounds.size)
        assertTrue(physicalRounds.none {
            it.firstEffect.effect in CardEffectReadiness.pending ||
                it.secondEffect.effect in CardEffectReadiness.pending
        })
        physicalRounds.groupingBy { it.name }.eachCount().forEach { (id, count) ->
            assertTrue(count <= requireNotNull(rounds.getCard(id)).quantity)
        }
        assertEquals(3, physicalRounds.count { it.type == RoundCardType.BATTLE })
    }
}
