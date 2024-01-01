from src.decision.base.DecideDeclareBase import DecideDeclareBase
from src.decision.base.DecideSupportBase import DecideSupportBase
from src.decision.validate.DecideDeclareValidate import DecideDeclareValidate
from src.decision.validate.DecideSupportValidate import DecideSupportValidate
from src.data.Game import Game


class Decisions:

    def __init__(self):

        self.declare: DecideDeclareBase = DecideDeclareValidate()
        self.support: DecideSupportBase = DecideSupportValidate()

    def set_game(self, game: Game):
        self.declare.game = game
        self.support.game = game



