from typing import List
from src.DieValues import DieValues
from src.Die import Die


class DieCollection:

    def __init__(self, sides=None):
        if sides is None:
            sides = []
        self.sides: List[int] = sides

    def add_die(self, sides: int):
        self.sides.append(sides)

    def remove_die(self, side: int):
        if side in self.sides:
            self.sides.remove(side)

    def roll(self) -> DieValues:
        values = DieValues()
        for sides in self.sides:
            values.add(Die.factory(sides).roll())
        return values

    @property
    def min_value(self) -> int:
        return len(self.sides)

    @property
    def max_value(self) -> int:
        return sum(self.sides)

    @property
    def num_dice(self) -> int:
        return len(self.sides)

    @property
    def average(self) -> float:
        total = 0
        for side in self.sides:
            total += (side + 1) / 2
        return total

    @property
    def best(self) -> int:
        best = 0
        for side in self.sides:
            if side > best:
                best = side
        return best

    def __str__(self) -> str:
        return f"DieCollection({str(self.sides)})"
