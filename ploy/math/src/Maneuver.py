from typing import Optional

from src.DieValues import DieValues
from src.CardType import CardType
from src.Deck import Deck


class Maneuver:

    def __init__(self):
        self.card_type: Optional[CardType] = None

    def adjust(self, dice: DieValues, level: int):
        pass

    def add_cards(self, deck: Deck):
        pass

    @property
    def can_level(self) -> bool:
        return True

    def __str__(self) -> str:
        return f"{self.card_type}"

    def __eq__(self, other):
        if isinstance(other, Maneuver):
            return self.card_type == other.card_type
        return False
