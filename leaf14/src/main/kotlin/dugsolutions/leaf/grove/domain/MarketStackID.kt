package dugsolutions.leaf.grove.domain

enum class MarketStackType {
    ROOT,
    VINE,
    FLOWER,
    WISP
}

enum class MarketStackID(val type: MarketStackType) {
    ROOT_1(MarketStackType.ROOT),
    ROOT_2(MarketStackType.ROOT),
    ROOT_3(MarketStackType.ROOT),
    FLOWER_1(MarketStackType.FLOWER),
    FLOWER_2(MarketStackType.FLOWER),
    FLOWER_3(MarketStackType.FLOWER),
    WILD(MarketStackType.VINE),
    WISP(MarketStackType.WISP)
}
