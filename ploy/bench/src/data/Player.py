# package src.data
from typing import List
from src.data.ManeuverPlate import ManeuverPlate
from src.data.Deck import Deck


class Player:

    plate: ManeuverPlate
    energy: int
    pips: int
    stash: Deck
    draw: Deck
    fatal_received: bool

    def __init__(self):
        self.plate = ManeuverPlate()
        self.energy = 20
        self.pips = 0
        self.draw = Deck()
        self.stash = Deck()
        self.fatal_received = False

    def nn_next_cards(self, size: int) -> List[int]:
        return self.draw.nn_next_cards(size)

    @property
    def stash_cards_total(self) -> int:
        return self.stash.cards_total
