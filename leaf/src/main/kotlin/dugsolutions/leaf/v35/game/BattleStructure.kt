package dugsolutions.leaf.v35.game

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

/** Selects the v38 three-Strike Battle or the preserved pre-v38 whole-Battle rules. */
enum class BattleStructure { THREE_ENGAGEMENTS, LEGACY_WHOLE_BATTLE }

/** Optional selection file. A missing file or property always means the v38 rules. */
object BattleStructureSettings {
    private const val KEY = "battle.structure"

    fun load(path: Path): BattleStructure {
        if (!Files.exists(path)) return BattleStructure.THREE_ENGAGEMENTS
        val properties = Properties()
        Files.newInputStream(path).use(properties::load)
        val raw = properties.getProperty(KEY)?.trim().orEmpty()
        if (raw.isEmpty()) return BattleStructure.THREE_ENGAGEMENTS
        return try {
            BattleStructure.valueOf(raw)
        } catch (error: IllegalArgumentException) {
            val hint = if (raw == "CANONICAL") {
                " (renamed: use LEGACY_WHOLE_BATTLE to reproduce pre-v38 Battle rules)"
            } else ""
            throw IllegalArgumentException("Unsupported $KEY '$raw' in $path$hint", error)
        }
    }
}
