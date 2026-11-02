package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.game.BattleStructure
import dugsolutions.leaf.v35.game.BattleStructureSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import java.nio.file.Files

class BattleStructureRoutingTest {
    @Test fun adapterIsDiscoverableInSimulationRuntime() {
        val providers = java.util.ServiceLoader.load(
            dugsolutions.leaf.v35.game.round.BattleStructureProvider::class.java
        ).toList()
        assertNotNull(providers.firstOrNull { it is SimulationBattleStructureProvider })
    }

    @Test fun absentSettingsDefaultToCanonical() {
        val dir = Files.createTempDirectory("battle-structure")
        try {
            assertEquals(BattleStructure.CANONICAL, BattleStructureSettings.load(dir.resolve("missing.properties")))
        } finally { Files.deleteIfExists(dir) }
    }

    @Test fun bothSelectionsAndInvalidSelection() {
        val file = Files.createTempFile("battle-structure", ".properties")
        try {
            Files.writeString(file, "battle.structure=CANONICAL\n")
            assertEquals(BattleStructure.CANONICAL, BattleStructureSettings.load(file))
            Files.writeString(file, "battle.structure=THREE_ENGAGEMENTS\n")
            assertEquals(BattleStructure.THREE_ENGAGEMENTS, BattleStructureSettings.load(file))
            Files.writeString(file, "battle.structure=UNKNOWN\n")
            assertFailsWith<IllegalArgumentException> { BattleStructureSettings.load(file) }
        } finally { Files.deleteIfExists(file) }
    }
}
