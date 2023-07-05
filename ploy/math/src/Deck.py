from typing import List
from src.DieCollection import DieCollection


class Deck:

    def __init__(self, hand: List[int], draw: List[int]):
        self.hand = DieCollection(hand)
        self.draw = DieCollection(draw)
