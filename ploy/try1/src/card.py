from enum import Enum
from typing import List, Tuple


class Card:

    def __eq__(self, other):
        return type(self) == type(other)


class Maneuver(Card):

    sides: int

    def __init__(self, sides: int):
        self.sides = sides


class Size(Enum):
    SMALL = 1
    MEDIUM = 2
    LARGE = 3


class Armor(Card):
    pass


class Backpack(Card):
    pass


# Reroll a die, take the higher value.
class BustACut(Card):
    pass


class CounterBlow(Card):
    pass


class Feint(Card):
    pass


class FreshPiercespective(Card):
    pass


class HoldYourPierce(Card):
    pass


class PoundingFlurry(Card):
    pass


# Force a max value for one die
class Precision(Card):
    pass


# Faction Matching: 2->d4, 3->d6, 4->d12, 5->d20.
class ShearMeOut(Card):

    def compute_sides(self, cards: List[Card]) -> int:
        count = 0
        for check in cards:
            if isinstance(check, Maneuver):
                count = count + 1
        if count == 0:
            return 0
        elif count == 1:
            return 4
        elif count == 2:
            return 6
        elif count == 3:
            return 12
        return 20


class Shield(Card):
    size: Size

    def __init__(self, size: Size):
        self.size = size



