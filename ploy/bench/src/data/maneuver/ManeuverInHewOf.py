from src.engine.DieValues import DieValues


# Reroll all one’s rolled.
def maneuver_in_hew_of(values: DieValues):
    for die in values.values:
        if die.value == 1:
            die.roll()
