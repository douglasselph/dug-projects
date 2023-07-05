from typing import List, Tuple, Any

from src.card import *
from src.intentions import Intentions

class Hand:

    cards: List[Card] = []

    intentions: Intentions = Intentions()

    def __init__(self, cards: List[Card]):
        self.cards = cards

    @property
    def has_cards(self) -> bool:
        return len(self.cards) > 0

    def min(self) -> int:
        count = 0
        pick, rest = self._split_precision()
        for card in pick:
            sides = self._sides(card)
            if sides > 0:
                count += sides
        for card in rest:
            sides = self._sides(card)
            if sides > 0:
                count += 1
        return count

    def max(self) -> int:
        count = 0
        for card in self.cards:
            sides = self._sides(card)
            if sides > 0:
                count += sides
        return count

    def average(self) -> float:
        count: float = 0
        pick, rest = self._split_precision()
        for card in pick:
            sides = self._sides(card)
            if sides > 0:
                count += sides
        rest_sides = self._sides_of(rest)
        if self.intentions.bust_a_cut:
            pass
        else:
            for side in rest_sides:
                count += (1.0 + side) / 2.0
        return count

    def use_next_turn_instead(self) -> List[Card]:
        rebuilt = []
        reserve = []
        for card in self.cards:
            if isinstance(card, FreshPiercespective) or isinstance(card, HoldYourPierce):
                reserve.append(card)
            else:
                rebuilt.append(card)
        self.cards = rebuilt
        return reserve

    #
    # SUPPORT
    #
    def _sides_of(self, items: List[Card]) -> List[int]:
        result: List[int] = []
        for card in items:
            sides = self._sides(card)
            if sides > 0:
                result.append(sides)
        return result

    def _sides(self, card: Card) -> int:
        if isinstance(card, ShearMeOut):
            return card.compute_sides(self.cards)
        elif isinstance(card, Maneuver):
            return card.sides
        return 0

    def _count_type(self, card_type: Any) -> int:
        count = 0
        for card in self.cards:
            if isinstance(card, card_type):
                count += 1
        return count

    def _split_precision(self) -> Tuple[List[Card], List[Card]]:
        count_precision = self._count_type(Precision)
        if count_precision > 0:
            self.cards.sort(key=self._sides, reverse=True)
            pick = self.cards[:count_precision]
            rest = self.cards[count_precision:]
            return pick, rest
        else:
            return [], self.cards

