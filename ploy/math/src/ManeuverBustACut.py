from typing import List

from Maneuver import Maneuver
from CardType import CardTypeManeuver
from src.DieValues import DieValues


class ManeuverBustACut(Maneuver):

    def __init__(self):
        super().__init__()
        self.card_type = CardTypeManeuver.BUST_A_CUT

    #
    # Feature: You may reroll any one die. You must keep the second roll.
    #
    # Bust A Cut Strategy rules:
    #   Given a set of dice of a variety of sides:
    #     - If all dice are greater than their EV then all dice values are kept.
    #     - If exactly one die is less than it’s EV then that die is rerolled.
    #     - If multiple dice roll under their expected values (EVs),
    #       re-roll the one with the largest shortfall from its EV.
    #     - If multiple dice satisfy the condition of the last step (a tie),
    #       then choose the die with the largest number of sides
    #   The end result of any strategy is to keep track of a final totalled EV.
    #
    def adjust(self, dice: DieValues, level: int):
        for count in range(level):
            self._adjust(dice)

    @staticmethod
    def _adjust(dice: DieValues):
        deviations: List[float] = []
        count_less_than_average: List[int] = []

        for index, die in enumerate(dice.values):
            average = die.average
            value = die.value
            deviation = value - average
            deviations.append(deviation)
            if deviation < 0:
                count_less_than_average.append(index)

        if len(count_less_than_average) == 0:
            return

        if len(count_less_than_average) == 1:
            dice.values[count_less_than_average[0]].roll()
            return

        # Find the largest deviation
        min_value = min(deviations)
        min_indexes = [index for index, value in enumerate(deviations) if value == min_value]

        # Extract 'sides' from the Die objects
        sides = [die.sides for die in dice.values]

        # Grab the values from 'sides' using the indexes in 'min_indexes'
        corresponding_sides = [sides[i] for i in min_indexes]

        # Find the maximum value among the corresponding sides
        max_side = max(corresponding_sides)

        # Find all indexes in 'min_indexes' whose corresponding values in 'sides' are equal
        # to this maximum value
        final_indexes = [i for i in min_indexes if sides[i] == max_side]

        # For each final index value, re-roll
        for index in final_indexes:
            dice.values[index].roll()
