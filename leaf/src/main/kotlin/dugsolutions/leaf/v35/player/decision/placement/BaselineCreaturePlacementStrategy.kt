package dugsolutions.leaf.v35.player.decision.placement

/**
 * @deprecated The old Baseline name referred to the deterministic mechanical
 * control policy. Use [MechanicalCreaturePlacementStrategy] for engine/integration control code, or the
 * HumanBaseline layer for simulation behavior.
 */
@Deprecated(
    message = "Use MechanicalCreaturePlacementStrategy for the mechanical control policy",
    replaceWith = ReplaceWith(
        "MechanicalCreaturePlacementStrategy()",
        "dugsolutions.leaf.v35.player.decision.mechanical.placement.MechanicalCreaturePlacementStrategy"
    )
)
typealias BaselineCreaturePlacementStrategy =
    dugsolutions.leaf.v35.player.decision.mechanical.placement.MechanicalCreaturePlacementStrategy
