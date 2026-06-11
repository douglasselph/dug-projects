package dugsolutions.leaf.v30.di

import dugsolutions.leaf.v30.chronicle.decision.DecisionCountLog
import dugsolutions.leaf.v30.chronicle.decision.DecisionCountType
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.di.PlayerFactory
import org.junit.jupiter.api.Test
import org.koin.dsl.koinApplication
import kotlin.test.assertEquals

class AppModuleTest {

    @Test
    fun playerFactory_usesDecisionCountingDirector() {
        val app = koinApplication { modules(appModules) }
        val playerFactory = app.koin.get<PlayerFactory>()
        val decisionCountLog = app.koin.get<DecisionCountLog>()
        val player = playerFactory()

        player.decisionDirector.chooseCritter(
            Decision.ChooseCritter(
                player = player,
                availableCritters = listOf(Critter.BEE, Critter.WORM)
            )
        )

        val entry = decisionCountLog.getEntries().single()
        assertEquals(player.id, entry.playerId)
        assertEquals(DecisionCountType.CHOOSE_CRITTER, entry.type)
        assertEquals(2, entry.count)
    }
}
