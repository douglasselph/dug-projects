from Maneuver import Maneuver
from src.DieValues import DieValues
from CardType import CardTypeManeuver


class ManeuverPrecision(Maneuver):

    def __init__(self):
        super().__init__()
        self.card_type = CardTypeManeuver.PRECISION

    #
    # Feature: After rolling, choose a die in the same group, it is now at max. A d20 cost 1 Energy.
    #
    # Precision Strategy rules:
    #   Given a set of dice of a variety of sides:
    #     - Find the value which if increased to its max value yields the greatest increase to the overall value.
    #
    def adjust(self, dice: DieValues, level: int):
        for count in range(level):
            self._adjust(dice)

    @staticmethod
    def _adjust(dice: DieValues):
        best_index = -1
        best_value = 0
        unmodified_total = dice.total

        for index, die in enumerate(dice.values):
            modified_total = unmodified_total - die.value + die.max_value
            improved_value = modified_total - unmodified_total
            if improved_value > best_value:
                best_index = index
                best_value = improved_value

        if best_index >= 0:
            dice.values[best_index].value = dice.values[best_index].max_value