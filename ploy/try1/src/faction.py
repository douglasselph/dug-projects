from enum import Enum


class Faction(Enum):

    SLASH = 1
    FANG = 2
    THORN = 3
    RAVEN = 4

    def __repr__(self) -> str:
        if self == Faction.SLASH:
            return "SLASH"
        elif self == Faction.FANG:
            return "FANG"
        elif self == Faction.THORN:
            return "THORN"
        elif self == Faction.RAVEN:
            return "RAVEN"



