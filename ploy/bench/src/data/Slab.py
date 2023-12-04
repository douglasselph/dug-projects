# package src.data
from enum import Enum


class SlabID(Enum):
    NONE = 0
    COMMON_DRAW = 1
    COMMON_FACE_UP = 2
    SELF_STASH_DRAW = 3
    SELF_STASH_FACE_UP = 4
    OPPONENT_FACE_UP = 5

