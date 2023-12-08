from src.data.die.DieValues import DieValues
from src.data.Card import DieSides


# Add two to the value of each D4 rolled
# TODO: Apply this new rule to the actual cards.
class ManeuverNickToDeath:

    @staticmethod
    def apply(dice: DieValues):
        for die in dice.values:
            if die.sides == DieSides.D4:
                die.value += 2

