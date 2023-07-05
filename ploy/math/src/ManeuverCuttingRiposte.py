from Maneuver import Maneuver
from src.CardType import CardTypeManeuver
from src.DieValues import DieValues
from src.Deck import Deck
from src.Die import Die


class ManeuverCuttingRiposte(Maneuver):

    def __init__(self):
        super().__init__()
        self.card_type = CardTypeManeuver.CUTTING_RIPOSTE
        self.d6 = Die(6)

    #
    # CuttingRiposte Strategy rules:
    #    The benchmark system we have doesn't have any explicit concept of an opponent's hand.
    #    So just rolling a d4 in order to determine which die is awarded (can be none).
    #
    # Old Feature: If your opponent has a D20, then gain a D10.
    # Or, if your opponent has a D12, then gain a D6.
    # Or, if your opponent has a D8, then gain a D4.
    #
    # Old Feature: Capture an opponent's die which you exceed and then reroll that die.
    #
    # Old Feature: Before the roll, downgrade the number of sides of one of your opponent’s dice.
    # For example, a D20 to a D12, or a D12 to a D10.
    #
    def adjust(self, dice: DieValues, level: int):
        for count in range(level):
            self._adjust(dice)

    @staticmethod
    def _adjust(dice: DieValues):
        copy = dice.dup()
        copy.roll()
        best_sides = 0
        for die in copy.values:
            if die.value == 1 and die.sides > best_sides:
                best_sides = die.sides
        if best_sides > 0:
            dice.add(Die(best_sides).roll())
