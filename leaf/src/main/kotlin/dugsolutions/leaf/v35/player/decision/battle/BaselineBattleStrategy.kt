package dugsolutions.leaf.v35.player.decision.battle

/**
 * @deprecated The old Baseline name referred to the deterministic mechanical
 * control policy. Use [MechanicalBattleStrategy] for engine/integration control code, or the
 * HumanBaseline layer for simulation behavior.
 */
@Deprecated(
    message = "Use MechanicalBattleStrategy for the mechanical control policy",
    replaceWith = ReplaceWith(
        "MechanicalBattleStrategy()",
        "dugsolutions.leaf.v35.player.decision.mechanical.battle.MechanicalBattleStrategy"
    )
)
typealias BaselineBattleStrategy =
    dugsolutions.leaf.v35.player.decision.mechanical.battle.MechanicalBattleStrategy
