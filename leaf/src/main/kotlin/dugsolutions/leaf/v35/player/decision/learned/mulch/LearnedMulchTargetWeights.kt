package dugsolutions.leaf.v35.player.decision.learned.mulch

import java.nio.file.Files
import java.nio.file.Path

data class LearnedMulchTargetProvenance(
    val trainingStatus: String = "untrained",
    val roundPattern: String = "unknown",
    val playerCount: Int? = null,
    val generations: Int? = null,
    val gamesPerPolicy: Int? = null,
    val population: Int? = null,
    val evolutionSeed: Long? = null,
    val mechanicalSeedStart: Long? = null,
    val strategySeedStart: Long? = null,
    val fitness: Double? = null
)

class LearnedMulchTargetWeights private constructor(
    private val values: DoubleArray,
    val provenance: LearnedMulchTargetProvenance
) {
    operator fun get(feature: MulchTargetFeature): Double = values[feature.ordinal]
    fun toDoubleArray() = values.copyOf()
    fun withProvenance(p: LearnedMulchTargetProvenance) = LearnedMulchTargetWeights(values.copyOf(), p)
    fun save(path: Path) {
        path.parent?.let(Files::createDirectories)
        val s = buildString {
            appendLine("formatVersion=1")
            appendLine("policy=mulch-target-v1")
            appendLine("trainingStatus=${provenance.trainingStatus}")
            appendLine("trainedRoundPattern=${provenance.roundPattern}")
            provenance.playerCount?.let { appendLine("trainingPlayerCount=$it") }
            provenance.generations?.let { appendLine("trainingGenerations=$it") }
            provenance.gamesPerPolicy?.let { appendLine("trainingGamesPerPolicy=$it") }
            provenance.population?.let { appendLine("trainingPopulation=$it") }
            provenance.evolutionSeed?.let { appendLine("trainingEvolutionSeed=$it") }
            provenance.mechanicalSeedStart?.let { appendLine("trainingMechanicalSeedStart=$it") }
            provenance.strategySeedStart?.let { appendLine("trainingStrategySeedStart=$it") }
            provenance.fitness?.let { appendLine("trainingFitness=$it") }
            MulchTargetFeature.entries.forEach { appendLine("${it.name}=${this@LearnedMulchTargetWeights[it]}") }
        }
        Files.writeString(path, s)
    }
    companion object {
        fun zeros(p: LearnedMulchTargetProvenance = LearnedMulchTargetProvenance()) = LearnedMulchTargetWeights(DoubleArray(MulchTargetFeature.entries.size), p)
        fun fromDoubleArray(v: DoubleArray, p: LearnedMulchTargetProvenance = LearnedMulchTargetProvenance()): LearnedMulchTargetWeights {
            require(v.size == MulchTargetFeature.entries.size)
            require(v.all { it.isFinite() })
            return LearnedMulchTargetWeights(v.copyOf(), p)
        }
        fun load(path: Path): LearnedMulchTargetWeights {
            val e = Files.readAllLines(path).filter { it.isNotBlank() && !it.trimStart().startsWith("#") }.associate { line ->
                val x=line.split('=',limit=2); require(x.size==2); x[0].trim() to x[1].trim()
            }
            require(e["formatVersion"]=="1" && e["policy"]=="mulch-target-v1")
            val v=DoubleArray(MulchTargetFeature.entries.size){i->e.getValue(MulchTargetFeature.entries[i].name).toDouble()}
            return LearnedMulchTargetWeights(v, LearnedMulchTargetProvenance(
                trainingStatus=e["trainingStatus"]?:"unknown", roundPattern=e["trainedRoundPattern"]?:"unknown",
                playerCount=e["trainingPlayerCount"]?.toInt(), generations=e["trainingGenerations"]?.toInt(),
                gamesPerPolicy=e["trainingGamesPerPolicy"]?.toInt(), population=e["trainingPopulation"]?.toInt(),
                evolutionSeed=e["trainingEvolutionSeed"]?.toLong(), mechanicalSeedStart=e["trainingMechanicalSeedStart"]?.toLong(),
                strategySeedStart=e["trainingStrategySeedStart"]?.toLong(), fitness=e["trainingFitness"]?.toDouble()
            ))
        }
    }
}
