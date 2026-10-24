package dugsolutions.leaf.simulation.v35.experiment.baseline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BaselineResyncCardBalanceOptionsTest {
    @Test fun preservesLegacyDefaults() {
        val o = BaselineResyncCardBalanceOptions.parse(emptyArray())
        assertEquals(0, o.start)
        assertEquals(100, o.games)
        assertEquals(4, o.players)
        assertEquals(120000L, o.baseSeed)
        assertEquals(220000L, o.strategySeed)
        assertEquals(320000L, o.groveSeed)
        assertEquals("data/research/resync/resync-current.csv", o.plantFile.toString())
        assertEquals("data/research/resync/round-resync-current.csv", o.roundFile.toString())
    }

    @Test fun parsesChunkInputsWithoutChangingGlobalSample() {
        val o = BaselineResyncCardBalanceOptions.parse(arrayOf(
            "--start=300", "--games=37", "--players=2", "--seed=55",
            "--strategy-seed=77", "--grove-seed=99", "--output=build/check.tsv"
        ))
        assertEquals(300, o.start)
        assertEquals(37, o.games)
        assertEquals(2, o.players)
        assertEquals(355L, o.baseSeed + o.start)
        assertEquals(377L, o.strategySeed + o.start)
        assertEquals("build/check.tsv", o.output.path)
    }

    @Test fun rejectsMalformedAndInvalidOptions() {
        assertFailsWith<IllegalArgumentException> { BaselineResyncCardBalanceOptions.parse(arrayOf("games=3")) }
        assertFailsWith<IllegalArgumentException> { BaselineResyncCardBalanceOptions.parse(arrayOf("--games=0")) }
        assertFailsWith<IllegalArgumentException> { BaselineResyncCardBalanceOptions.parse(arrayOf("--players=5")) }
    }
}
