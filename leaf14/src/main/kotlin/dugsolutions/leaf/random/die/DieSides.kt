package dugsolutions.leaf.random.die

enum class DieSides(val value: Int) {
    D4(4),
    D6(6),
    D8(8),
    D10(10),
    D12(12),
    D20(20);

    companion object {
        fun from(sides: Int): DieSides {
            return when (sides) {
                4 -> D4
                6 -> D6
                8 -> D8
                10 -> D10
                12 -> D12
                20 -> D20
                else -> throw Exception("Unknown number of sides: $sides")
            }
        }

    }

    override fun toString(): String {
        return "D$value"
    }

    val downgrade: DieSides?
        get() {
            return when(value) {
                4 -> null
                6 -> D4
                8 -> D6
                10 -> D8
                12 -> D10
                20 -> D12
                else -> null
            }
        }

    val upgrade: DieSides?
        get() {
            return when(value) {
                4 -> D6
                6 -> D8
                8 -> D10
                10 -> D12
                12 -> D20
                else -> null
            }
        }

}
