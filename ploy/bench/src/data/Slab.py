# package src.data
from enum import Enum
from src.data.Deck import Deck


class SlabID(Enum):
    NONE = 0
    COMMON_DRAW = 1
    COMMON_FACE_UP = 2
    SELF_STASH_DRAW = 3
    SELF_STASH_FACE_UP = 4
    OPPONENT_FACE_UP = 5


class Slab:

    def __init__(self):
        self._deck = Deck()
