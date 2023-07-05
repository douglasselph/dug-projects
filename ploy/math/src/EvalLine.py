from __future__ import annotations

from src.Deck import Deck
from Maneuver import Maneuver
from src.Stats import Stats
from src.DieValues import DieValues


#
# Evaluate a maneuver against a line of dice to be rolled stored against some stats.
#
class EvalLine:

    def __init__(self, maneuver: Maneuver, deck: Deck):
        self.maneuver = maneuver
        self.deck = deck
        self.stats: Stats = Stats()

    def reset(self):
        self.stats.reset()

    def add_cards(self):
        self.maneuver.add_cards(self.deck)

    def roll(self) -> DieValues:
        return self.deck.hand.roll()

    def adjust(self, dice: DieValues, level: int) -> DieValues:
        self.maneuver.adjust(dice, level)
        return dice

    def set_stats(self, dice: DieValues):
        self.stats.add(dice.total)

    @property
    def can_level(self) -> bool:
        return self.maneuver.can_level

    def __str__(self) -> str:
        return f"{str(self.maneuver)}([{self.deck.hand.num_dice}]: {self.deck.hand}) -> {self.stats}"
