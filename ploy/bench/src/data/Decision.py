# src.data
from enum import Enum


class DecisionLine(Enum):
    LINE_1 = 1
    LINE_2 = 2
    LINE_3 = 3
    LINE_4 = 4


class DecisionIntention(Enum):
    NONE = 0
    ATTACK = 1
    DEFEND = 2
    DEPLOY = 3

