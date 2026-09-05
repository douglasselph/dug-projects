package dugsolutions.leaf.v35.player.decision.buy

/**
 * @deprecated The old Baseline name referred to the deterministic mechanical
 * control policy. Use [MechanicalBuyStrategy] for engine/integration control code, or the
 * HumanBaseline layer for simulation behavior.
 */
@Deprecated(
    message = "Use MechanicalBuyStrategy for the mechanical control policy",
    replaceWith = ReplaceWith(
        "MechanicalBuyStrategy()",
        "dugsolutions.leaf.v35.player.decision.mechanical.buy.MechanicalBuyStrategy"
    )
)
typealias BaselineBuyStrategy =
    dugsolutions.leaf.v35.player.decision.mechanical.buy.MechanicalBuyStrategy
