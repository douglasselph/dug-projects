from src.die.DieCollection import DieCollection
from src.die.DieSides import DieSides


class Constants:

    max_hp = 10
    num_maneuvers = 3

    starting_die_collection = DieCollection([
        DieSides.D4,
        DieSides.D6,
        DieSides.D8,
        DieSides.D10,
        DieSides.D12,
        DieSides.D20
    ])
