from typing import Optional
from src.engine.DieValues import DieValues
from src.data.Card import DieSides
from src.engine.Die import Die


# Just once, when your opponent rolls a one, they retain that value; however,
# that specific die is captured and can be rerolled as part of one's own rolls.
def maneuver_keep_the_pierce(own: DieValues, opponent: DieValues):
    best: DieSides = DieSides.NONE
    for die in opponent:
        if die.value == 1:
            if best is None:
                best = die
            elif die.sides.sides > best.sides:
                best = die.sides
    if best != DieSides.NONE:
        own.add(Die(best).roll())
