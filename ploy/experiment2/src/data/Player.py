from src.data.Constants import Constants
from src.die.DieCollection import DieCollection
from src.data.ID import IntentionID
from src.die.DieSides import DieSides
from src.data.Moves import MoveDeclare


class Player:

    hp: int
    dice: DieCollection

    def __init__(self):
        self.hp = Constants.max_hp
        self.dice = Constants.starting_die_collection.dup()
        self.declaration: IntentionID = IntentionID.NONE
        self.declared_die: DieSides = DieSides.NONE

    @property
    def is_alive(self) -> bool:
        return self.hp > 0

    def new_maneuver(self):
        self.declaration = IntentionID.NONE
        self.declared_die = DieSides.NONE

    def apply_declaration(self, declaration: MoveDeclare):
        self.declaration = declaration.intention
        self.declared_die = declaration.die

    @property
    def dice_average(self) -> float:
        return self.dice.average

    @property
    def die_lowest(self) -> DieSides:
        return self.dice.lowest



