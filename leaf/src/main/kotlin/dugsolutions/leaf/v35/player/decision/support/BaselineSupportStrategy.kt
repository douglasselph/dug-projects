package dugsolutions.leaf.v35.player.decision.support

/**
 * @deprecated The old Baseline name referred to the deterministic mechanical
 * control policy. Use [MechanicalSupportStrategy] for engine/integration control code, or the
 * HumanBaseline layer for simulation behavior.
 */
@Deprecated(
    message = "Use MechanicalSupportStrategy for the mechanical control policy",
    replaceWith = ReplaceWith(
        "MechanicalSupportStrategy()",
        "dugsolutions.leaf.v35.player.decision.mechanical.support.MechanicalSupportStrategy"
    )
)
typealias BaselineSupportStrategy =
    dugsolutions.leaf.v35.player.decision.mechanical.support.MechanicalSupportStrategy
