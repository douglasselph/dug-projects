# package src.data
from typing import List
from src.data.ManeuverPlate import ManeuverPlate, IntentionID
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

    @property
    def line_sizes(self):
        return self.plate.line_sizes

    def line_intention_id(self, position: int) -> IntentionID:
        return self.plate.line_intention_id(position)

    def line_card_values(self, position: int) -> List[int]:
        return self.plate.line_card_values(position)

    @property
    def lines_num_cards(self) -> List[int]:
        return self.plate.lines_num_cards
