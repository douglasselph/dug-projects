from typing import List
from src.data.die.DieValues import DieValues
from src.data.die.Die import Die
from src.data.Card import DieSides


class DieCollection:

    sides: List[DieSides]

    def __init__(self, sides: List[DieSides]):
        self.sides: List[DieSides] = sides

    def add_die(self, sides: DieSides):
        self.sides.append(sides)

    def remove_die(self, side: DieSides):
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
        return sum(item.value for item in self.sides)

    @property
    def num_dice(self) -> int:
        return len(self.sides)

    @property
    def average(self) -> float:
        total = 0
        for side in self.sides:
            total += (side.value + 1) / 2
        return total

    @property
    def best(self) -> int:
        best = 0
        for side in self.sides:
            if side.value > best:
                best = side
        return best

    def __str__(self) -> str:
        return f"DieCollection({str(self.sides)})"
