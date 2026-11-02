package dugsolutions.leaf.v35.game

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

/** A per-game selection; canonical is always the default. */
enum class BattleStructure { CANONICAL, THREE_ENGAGEMENTS }

/** Optional research configuration; deliberately separate from Plant-card resync CSV. */
object BattleStructureSettings {
    fun load(path: Path): BattleStructure {
        if (!Files.exists(path)) return BattleStructure.CANONICAL
        val properties = Properties()
        Files.newInputStream(path).use(properties::load)
        val raw = properties.getProperty("battle.structure", "CANONICAL").trim()
        return try { BattleStructure.valueOf(raw) } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Unsupported battle.structure '$raw' in $path", error)
        }
    }
}
