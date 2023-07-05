from typing import Optional

from src.Maneuver import Maneuver
from src.DieValues import DieValues
from src.CardType import CardTypeManeuver
from src.Die import Die


class ManeuverKeepThePierce(Maneuver):

    def __init__(self):
        super().__init__()
        self.card_type = CardTypeManeuver.KEEP_THE_PIERCE
        self.opponent_dice: Optional[DieValues] = None

    #
    # Feature: Just once, when your opponent rolls a one, they retain that value;
    # however, that specific die is captured and can be rerolled as part of one's own rolls.
    #
    # KeepThePierce Strategy rules:
    #    This is a non-exact strategy since we do not have the opponents die rolls to work with.
    #    What I try here is to assume the opponent has the exact same set of dice we do.
    #    I then roll a copy of those, and add the higher die to the roll that was captured.
    #
    def adjust(self, dice: DieValues, level: int):
        if self.opponent_dice:
            opponent_dice = self.opponent_dice
        else:
            opponent_dice = dice.dup()
            opponent_dice.roll()
        best = 0
        for die in opponent_dice.values:
            if die.value == 1 and die.sides > best:
                best = die.sides
        if best > 1:
            dice.add(Die(best).roll())





