from Maneuver import Maneuver
from src.DieValues import DieValues
from src.CardType import CardTypeManeuver
from src.Die import Die


class ManeuverFeelingFeint(Maneuver):

    def __init__(self):
        super().__init__()
        self.card_type = CardTypeManeuver.FEELING_FEINT
        self.d6 = Die(6)

    #
    # Feature: You may move any face down card on another line into the line with this card and apply immediately
    # reveal and apply it.
    #
    # ToDieFour Strategy rules:
    #    This one is handled by adding an extra D4, D6, D8, D10, or D12 or D20.
    #    It attempts to simulate the versatility of this card. You can have a die committed toward a different
    #    intent, and effectively because of this card, it can be used for either intent. There is
    #    no direct way to simulate this with the benchmarks I have in place, so in general I treat it
    #    as if you are adding one of the other die randomly to whatever the current intent you are doing. Not great,
    #    but at least it reflects something.
    #
    def adjust(self, dice: DieValues, level: int):
        for count in range(level):
            self._adjust(dice)

    def _adjust(self, dice: DieValues):
        which = self.d6.roll().value
        sides: int
        if which == 1:
            sides = 4
        elif which == 2:
            sides = 6
        elif which == 3:
            sides = 8
        elif which == 4:
            sides = 10
        elif which == 5:
            sides = 12
        else:
            sides = 20
        dice.add(Die(sides).roll())

