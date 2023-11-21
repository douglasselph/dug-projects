# src.engine.maneuver
from src.data.Card import DieSides
from src.engine.DieCollection import DieCollection


# Before the roll, downgrade the number of sides of one of your opponent’s dice.
# For example, a D20 to a D12, or a D12 to a D10.
def maneuver_cutting_riposte(collection: DieCollection):
    best: DieSides = DieSides.NONE

    for sides in collection.sides:
        if best == DieSides.NONE or sides.value > best.value:
            best = sides

    if best != DieSides.NONE:
        rebuilt = []
        for sides in collection.sides:
            if sides == best:
                smaller = best.downgrade()
                if smaller != DieSides.NONE:
                    rebuilt.append(smaller)
            else:
                rebuilt.append(sides)

        collection.sides = rebuilt



