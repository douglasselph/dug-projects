package dugsolutions.leaf.market.domain

enum class MarketStackType {
    ROOT,
    CANOPY,
    VINE,
    MIXED,
    BLOOM
}

enum class MarketStackID(val type: MarketStackType) {
    ROOT_1(MarketStackType.ROOT),
    ROOT_2(MarketStackType.ROOT),
    CANOPY_1(MarketStackType.CANOPY),
    CANOPY_2(MarketStackType.CANOPY),
    VINE_1(MarketStackType.VINE),
    VINE_2(MarketStackType.VINE),
    JOINT_RCV(MarketStackType.MIXED),
    BLOOM_1(MarketStackType.BLOOM),
    BLOOM_2(MarketStackType.BLOOM),
    BLOOM_3(MarketStackType.BLOOM)
}