from enum import Enum


class PlayerID(Enum):
    NONE = 0
    PLAYER_1 = 1
    PLAYER_2 = 2


class IntentionID(Enum):
    NONE = 0
    ATTACK = 1
    DODGE = 2
