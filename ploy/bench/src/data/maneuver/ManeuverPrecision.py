from src.engine.DieValues import DieValues


# After rolling, choose a die in the same group, it is now at max. A D20 cost 1 Energy.
# Strategy: Choose die which improves the most. Return True of D20 selected.
def maneuver_precision(values: DieValues) -> bool:
    best_index: int = -1
    best_sides: int = 0
    for index, die in enumerate(values.values):
        max_sides = die.sides.sides
        value = die.value
        diff = max_sides - value
        if diff > best_sides:
            best_index = index
            best_sides = die.sides.sides

    flag = False
    if best_index >= 0:
        values.values[best_index].value = values.values[best_index].sides
        if values.values[best_index].sides == 20:
            flag = True
    return flag
