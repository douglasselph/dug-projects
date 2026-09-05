package dugsolutions.leaf.v35.player.decision.reward

/**
 * @deprecated The old Baseline name referred to the deterministic mechanical
 * control policy. Use [MechanicalRewardStrategy] for engine/integration control code, or the
 * HumanBaseline layer for simulation behavior.
 */
@Deprecated(
    message = "Use MechanicalRewardStrategy for the mechanical control policy",
    replaceWith = ReplaceWith(
        "MechanicalRewardStrategy()",
        "dugsolutions.leaf.v35.player.decision.mechanical.reward.MechanicalRewardStrategy"
    )
)
typealias BaselineRewardStrategy =
    dugsolutions.leaf.v35.player.decision.mechanical.reward.MechanicalRewardStrategy
