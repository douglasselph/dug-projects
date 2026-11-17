package dugsolutions.leaf.v35.player.decision.baseline

/** Human preferences, not expert-learned action weights or hard game constraints. */
enum class ResourceInclination {
    LIKE,
    NEUTRAL,
    AVOID
}

/** The three resource decisions that a Human Baseline personality may influence. */
enum class PersonalityResource {
    WATER,
    SUNLIGHT,
    MULCH
}

/** A soft desired holding, not a requirement to acquire or keep this many tokens. */
data class ResourceGoal(
    val inclination: ResourceInclination = ResourceInclination.NEUTRAL,
    val softTarget: Int = 1
) {
    init {
        require(softTarget >= 0) { "A resource's soft target cannot be negative" }
    }
}

/**
 * An immutable, per-player description of Human Baseline tendencies.
 *
 * These settings are *not* learned priority scores. Cultivation Round resource
 * acquisitions now use them as soft preferences after contextual scoring;
 * Support spending, Battle decisions, and Plant effects remain unchanged.
 *
 * [id] plus [description] give research manifests a reproducible human-readable
 * configuration without importing expert-learned weights.
 */
data class ResourcePersonality(
    val id: String,
    val water: ResourceGoal = ResourceGoal(),
    val sunlight: ResourceGoal = ResourceGoal(),
    val mulch: ResourceGoal = ResourceGoal()
) {
    companion object {
        val NEUTRAL: ResourcePersonality = ResourcePersonality(id = "neutral")

        val WATER_KEEPER: ResourcePersonality = ResourcePersonality(
            id = "water-keeper",
            water = ResourceGoal(ResourceInclination.LIKE, softTarget = 2)
        )

        val BATTLE_PREPARER: ResourcePersonality = ResourcePersonality(
            id = "battle-preparer",
            sunlight = ResourceGoal(ResourceInclination.LIKE, softTarget = 2),
            mulch = ResourceGoal(ResourceInclination.LIKE, softTarget = 2)
        )

        val WATER_MULCH_NO_SUNLIGHT: ResourcePersonality = ResourcePersonality(
            id = "water-mulch-no-sunlight",
            water = ResourceGoal(ResourceInclination.LIKE, softTarget = 2),
            sunlight = ResourceGoal(ResourceInclination.AVOID, softTarget = 0),
            mulch = ResourceGoal(ResourceInclination.LIKE, softTarget = 2)
        )

        /** Named defaults for repeatable research and future behavioral cohorts. */
        val NAMED: List<ResourcePersonality> = listOf(
            NEUTRAL,
            WATER_KEEPER,
            BATTLE_PREPARER,
            WATER_MULCH_NO_SUNLIGHT
        )
    }

    init {
        require(id.isNotBlank()) { "Resource personality id cannot be blank" }
        require(id.matches(Regex("[a-z][a-z0-9-]*"))) {
            "Resource personality id must be a lowercase slug: $id"
        }
    }

    fun goal(resource: PersonalityResource): ResourceGoal = when (resource) {
        PersonalityResource.WATER -> water
        PersonalityResource.SUNLIGHT -> sunlight
        PersonalityResource.MULCH -> mulch
    }

    /** Stable, legible value string for future trace and research provenance. */
    fun description(): String = listOf(
        "profile=$id",
        "water=${water.inclination}:${water.softTarget}",
        "sunlight=${sunlight.inclination}:${sunlight.softTarget}",
        "mulch=${mulch.inclination}:${mulch.softTarget}"
    ).joinToString(";")
}
