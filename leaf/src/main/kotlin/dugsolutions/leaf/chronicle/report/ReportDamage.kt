package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.domain.Moment

class ReportDamage {

    operator fun invoke(moment: Moment.DELIVER_DAMAGE): String {
        val lines = mutableListOf<String>()
        val sbuf = StringBuffer()
        sbuf.append("Player ${moment.defender.id} took ${moment.damageToDefender}")
        if (moment.deflectDamage > 0) {
            sbuf.append(" but deflected ${moment.deflectDamage}. ")
        }
        sbuf.append(" (pips ${moment.attackerPipTotal} vs ${moment.defenderPipTotal})")
        lines.add(sbuf.toString())
        return lines.joinToString(",")
    }

    operator fun invoke(moment: Moment.THORN_DAMAGE): String {
        val lines = mutableListOf<String>()
        if (moment.thornDamage > 0) {
            lines.add("Player ${moment.player.id} took thorn damage of ${moment.thornDamage}")
        }
        return lines.joinToString(",")
    }
}
