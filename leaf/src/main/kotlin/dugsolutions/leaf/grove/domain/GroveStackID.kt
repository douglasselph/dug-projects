package dugsolutions.leaf.grove.domain

enum class MarketStackType {
    ROOT,
    CANOPY,
    VINE,
    MIXED,
    FLOWER
}

enum class MarketStackID(val type: MarketStackType) {
    ROOT_1(MarketStackType.ROOT),
    ROOT_2(MarketStackType.ROOT),
    CANOPY_1(MarketStackType.CANOPY),
    CANOPY_2(MarketStackType.CANOPY),
    VINE_1(MarketStackType.VINE),
    VINE_2(MarketStackType.VINE),
    JOINT_RCV(MarketStackType.MIXED),
    FLOWER_1(MarketStackType.FLOWER),
    FLOWER_2(MarketStackType.FLOWER),
    FLOWER_3(MarketStackType.FLOWER),
}
