from src.data.die.DieValues import DieValues
from src.data.die.Die import DieSides


# After rolling, choose a die in the same group, it is now at max. A D20 cost 1 Energy.
# Strategy: Choose die which improves the most. Return True of D20 selected.
class ManeuverPrecision:

    def __init__(self, d20_okay: bool):
        self.d20_okay = d20_okay

    #
    # Return true if d20 selected.
    #
    def apply(self, values: DieValues) -> bool:
        best_index: int = -1
        best_sides: int = 0
        for index, die in enumerate(values.values):
            max_sides = die.sides.sides
            value = die.value
            diff = max_sides - value
            if diff > best_sides:
                if self.d20_okay or die.sides != DieSides.D20:
                    best_index = index
                    best_sides = die.sides.sides

        flag = False
        if best_index >= 0:
            values.values[best_index].value = values.values[best_index].sides.value
            if values.values[best_index].sides == DieSides.D20:
                flag = True
        return flag
