package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.simulation.v35.experiment.orchestration.ExperimentChunk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class BaselineChunkCheckpointTest {
    private val header = "sample\tplayers\tcard"

    @Test fun `only fully validated chunks may be skipped`() {
        val dir = Files.createTempDirectory("checkpoint-test-").toFile()
        try {
            val chunk = ExperimentChunk(1, 7, 9)
            val state = BaselineChunkCheckpoint(dir, "fingerprint-1", 2, header)
            assertFalse(state.isComplete(chunk))
            state.temporary(chunk).writeText("$header\n7\t4\tA\n7\t4\tB\n8\t4\tA\n8\t4\tB\n")
            assertFalse(state.isComplete(chunk))
            state.commit(chunk)
            assertTrue(state.isComplete(chunk))
            assertFalse(BaselineChunkCheckpoint(dir, "other-inputs", 2, header).isComplete(chunk))
            state.tsv(chunk).appendText("corrupted\n")
            assertFalse(state.isComplete(chunk))
        } finally { dir.deleteRecursively() }
    }

    @Test fun `partial output cannot publish completion`() {
        val dir = Files.createTempDirectory("checkpoint-partial-").toFile()
        try {
            val chunk = ExperimentChunk(1, 0, 2)
            val state = BaselineChunkCheckpoint(dir, "fingerprint", 2, header)
            state.temporary(chunk).writeText("$header\n0\t4\tA\n0\t4\tB\n")
            assertThrows(IllegalArgumentException::class.java) { state.commit(chunk) }
            assertFalse(state.marker(chunk).exists())
            assertTrue(state.temporary(chunk).exists())
        } finally { dir.deleteRecursively() }
    }

    @Test fun `all samples must be in order`() {
        val dir = Files.createTempDirectory("checkpoint-order-").toFile()
        try {
            val chunk = ExperimentChunk(1, 10, 12)
            val state = BaselineChunkCheckpoint(dir, "fingerprint", 2, header)
            state.temporary(chunk).writeText("$header\n10\t4\tA\n11\t4\tA\n10\t4\tB\n11\t4\tB\n")
            assertFalse(state.validTsv(state.temporary(chunk), chunk))
        } finally { dir.deleteRecursively() }
    }
}
