from __future__ import annotations
from typing import List
from src.die.Die import Die


class DieValues:

    def __init__(self):
        self.values: List[Die] = []

    def add(self, value: Die) -> DieValues:
        self.values.append(value)
        return self

    def dup(self) -> DieValues:
        copy = DieValues()
        for die in self.values:
            copy.add(Die.factory(die.sides))
        return copy

    def roll(self):
        for die in self.values:
            die.roll()

    @property
    def total(self) -> int:
        total = 0
        for die in self.values:
            total += die.value
        return total

    @property
    def num(self) -> int:
        return len(self.values)

    def __eq__(self, other):
        if isinstance(other, DieValues):
            for index, value in enumerate(other.values):
                if self.values[index] != value:
                    return False
            return True
        return False

    def __iter__(self):
        return iter(self.values)

    def __getitem__(self, index: int) -> Die:
        return self.values[index]

    def __setitem__(self, index: int, value: Die):
        self.values[index] = value
