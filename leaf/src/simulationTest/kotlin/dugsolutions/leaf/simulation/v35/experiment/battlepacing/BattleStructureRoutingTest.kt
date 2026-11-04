package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.game.BattleStructure
import dugsolutions.leaf.v35.game.BattleStructureSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.nio.file.Files

class BattleStructureRoutingTest {
    @Test fun adapterIsDiscoverableInSimulationRuntime() {
        val providers = java.util.ServiceLoader.load(
            dugsolutions.leaf.v35.game.round.BattleStructureProvider::class.java
        ).toList()
        assertNotNull(providers.firstOrNull { it is SimulationBattleStructureProvider })
    }

    @Test fun absentSettingsFileDefaultsToThreeEngagements() {
        val dir = Files.createTempDirectory("battle-structure")
        try {
            assertEquals(
                BattleStructure.THREE_ENGAGEMENTS,
                BattleStructureSettings.load(dir.resolve("missing.properties"))
            )
        } finally { Files.deleteIfExists(dir) }
    }

    @Test fun emptyFileAndAbsentOrEmptyPropertyDefaultToThreeEngagements() {
        val file = Files.createTempFile("battle-structure", ".properties")
        try {
            for (content in listOf("", "# no override\n", "some.other.setting=1\n", "battle.structure=  \n")) {
                Files.writeString(file, content)
                assertEquals(BattleStructure.THREE_ENGAGEMENTS, BattleStructureSettings.load(file))
            }
        } finally { Files.deleteIfExists(file) }
    }

    @Test fun explicitSelectionsAndInvalidSelection() {
        val file = Files.createTempFile("battle-structure", ".properties")
        try {
            Files.writeString(file, "battle.structure=LEGACY_WHOLE_BATTLE\n")
            assertEquals(BattleStructure.LEGACY_WHOLE_BATTLE, BattleStructureSettings.load(file))
            Files.writeString(file, "battle.structure=THREE_ENGAGEMENTS\n")
            assertEquals(BattleStructure.THREE_ENGAGEMENTS, BattleStructureSettings.load(file))
            Files.writeString(file, "battle.structure=CANONICAL\n")
            val legacyNameError = assertFailsWith<IllegalArgumentException> {
                BattleStructureSettings.load(file)
            }
            assertTrue(legacyNameError.message.orEmpty().contains("LEGACY_WHOLE_BATTLE"))
            Files.writeString(file, "battle.structure=UNKNOWN\n")
            assertFailsWith<IllegalArgumentException> { BattleStructureSettings.load(file) }
        } finally { Files.deleteIfExists(file) }
    }
}
