package dugsolutions.leaf.v35.player.decision.effect

/**
 * @deprecated The old Baseline name referred to the deterministic mechanical
 * control policy. Use [MechanicalEffectStrategy] for engine/integration control code, or the
 * HumanBaseline layer for simulation behavior.
 */
@Deprecated(
    message = "Use MechanicalEffectStrategy for the mechanical control policy",
    replaceWith = ReplaceWith(
        "MechanicalEffectStrategy()",
        "dugsolutions.leaf.v35.player.decision.mechanical.effect.MechanicalEffectStrategy"
    )
)
typealias BaselineEffectStrategy =
    dugsolutions.leaf.v35.player.decision.mechanical.effect.MechanicalEffectStrategy
