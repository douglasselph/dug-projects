package dugsolutions.leaf.v35.player.decision.cultivation

/**
 * @deprecated The old Baseline name referred to the deterministic mechanical
 * control policy. Use [MechanicalCultivationStrategy] for engine/integration control code, or the
 * HumanBaseline layer for simulation behavior.
 */
@Deprecated(
    message = "Use MechanicalCultivationStrategy for the mechanical control policy",
    replaceWith = ReplaceWith(
        "MechanicalCultivationStrategy()",
        "dugsolutions.leaf.v35.player.decision.mechanical.cultivation.MechanicalCultivationStrategy"
    )
)
typealias BaselineCultivationStrategy =
    dugsolutions.leaf.v35.player.decision.mechanical.cultivation.MechanicalCultivationStrategy
