from Maneuver import Maneuver
from src.DieValues import DieValues
from CardType import CardTypeManeuver
from Die import Die


class ManeuverInHewOf(Maneuver):

    def __init__(self):
        super().__init__()
        self.card_type = CardTypeManeuver.IN_HEW_OF

    #
    # Feature: D4. Reroll all one’s rolled.
    #
    # In Hew Of Strategy rules:
    #   Given a set of dice of a variety of sides:
    #     If a one is found on at least one die, re-roll that die.
    #
    def adjust(self, dice: DieValues, level: int):
        for count in range(level):
            self._adjust(dice)

    @staticmethod
    def _adjust(dice: DieValues):
        dice.add(Die(4).roll())
        for die in dice.values:
            if die.value == 1:
                die.roll()



