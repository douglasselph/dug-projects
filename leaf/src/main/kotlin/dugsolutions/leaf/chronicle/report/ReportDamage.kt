package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.domain.Moment

class ReportDamage {

    operator fun invoke(moment: Moment.DELIVER_DAMAGE): String {
        val lines = mutableListOf<String>()
        if (moment.damageToDefender > 0) {
            lines.add("Player ${moment.defender.id} took ${moment.damageToDefender} (pips ${moment.attackerPipTotal} vs ${moment.defenderPipTotal})")
        }
        if (moment.damageToAttacker > 0) {
            lines.add("Player ${moment.attacker.id} took back ${moment.damageToAttacker}")
        }
        return lines.joinToString(",")
    }
}
