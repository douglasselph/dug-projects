from src.engine.DieCollection import DieCollection
from src.engine.Die import Die
from src.data.Card import DieSides


# Roll a D4 to determine how many D4’s are added to the roll.
def maneuver_nick_to_death(dice: DieCollection):
    d4_value = Die(DieSides.D4).roll()
    for i in range(d4_value.value):
        dice.add_die(DieSides.D4)
