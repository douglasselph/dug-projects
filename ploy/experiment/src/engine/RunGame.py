from src.data.Game import Game
from src.decision.Decisions import Decisions
from src.engine.RunBout import RunBout


class RunGame:

    def __init__(self, game: Game, decisions: Decisions):
        self.game = game
        self.decisions = decisions
        self.decisions.set_game(game)

    @property
    def end_of_game(self) -> bool:
        return not self.game.all_players_are_alive

    def run_bout(self):
        bout = RunBout(self.game, self.decisions)

        while bout.has_maneuvers_left:
            bout.new_maneuver()
            bout.declare_maneuver()
            bout.support_maneuver()
            bout.execute_maneuver()

        self.game.add(bout.moves)

