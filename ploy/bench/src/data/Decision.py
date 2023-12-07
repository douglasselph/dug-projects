# src.data
from __future__ import annotations
from enum import Enum
from typing import Optional


class DecisionLine(Enum):
    LINE_1 = 1
    LINE_2 = 2
    LINE_3 = 3
    LINE_4 = 4

    @property
    def pos(self) -> int:
        return self.value - 1


class DecisionIntention(Enum):
    NONE = 0
    ATTACK = 1
    DEFEND = 2
    DEPLOY = 3


class DecisionDeck(Enum):
    NONE = 0
    PERSONAL_STASH_FACE_UP = 1
    PERSONAL_STASH_DRAW = 2
    COMMON_FACE_UP = 3
    COMMON_DRAW = 4
    OPPONENT_STASH_FACE_UP = 5


def coin_for(value) -> Optional[DecisionIntention]:
    for coin in DecisionIntention:
        if coin.value == value:
            return coin
    return None
