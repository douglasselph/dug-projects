from __future__ import annotations
import random
from src.die.DieSides import DieSides


class Die:
    sides: DieSides
    value: int

    def __init__(self, sides: DieSides, value: int = 0):
        super().__init__()
        self.sides = sides
        self.value = value

    @property
    def average(self) -> float:
        return (self.sides.value + 1) / 2

    def roll(self) -> Die:
        self.value = self.rand()
        return self

    @property
    def max_value(self) -> int:
        return self.sides.value

    def __eq__(self, other):
        if isinstance(other, Die):
            if self.sides == other.sides and self.value == other.value:
                return True
        return False

    @staticmethod
    def factory(sides: DieSides, value: int = 0) -> Die:
        return Die(sides, value)

    def rand(self) -> int:
        return random.randint(1, self.sides.value)
