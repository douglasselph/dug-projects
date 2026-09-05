package dugsolutions.leaf.v35.player.decision.wound

/**
 * @deprecated The old Baseline name referred to the deterministic mechanical
 * control policy. Use [MechanicalWoundStrategy] for engine/integration control code, or the
 * HumanBaseline layer for simulation behavior.
 */
@Deprecated(
    message = "Use MechanicalWoundStrategy for the mechanical control policy",
    replaceWith = ReplaceWith(
        "MechanicalWoundStrategy()",
        "dugsolutions.leaf.v35.player.decision.mechanical.wound.MechanicalWoundStrategy"
    )
)
typealias BaselineWoundStrategy =
    dugsolutions.leaf.v35.player.decision.mechanical.wound.MechanicalWoundStrategy
