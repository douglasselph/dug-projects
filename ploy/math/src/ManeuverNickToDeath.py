from Maneuver import Maneuver
from src.DieValues import DieValues
from src.CardType import CardTypeManeuver
from src.Die import Die


class ManeuverNickToDeath(Maneuver):

    def __init__(self):
        super().__init__()
        self.card_type = CardTypeManeuver.NICK_TO_DEATH
        self.d4 = Die.factory(4)

    #
    # Feature: Roll a D6 to determine how many D4’s are added to the roll.
    #
    # NickToDeath Strategy rules:
    #    Add D6 number of D4s and the added those.
    #
    def adjust(self, dice: DieValues, level: int):
        for count in range(level):
            self._adjust(dice)

    def _adjust(self, dice: DieValues):
        number_d4 = self.d4.roll().value
        for i in range(number_d4):
            dice.add(Die.factory(4).roll())
