from src.decision.Decisions import Decisions
from src.data.Game import Game
from src.engine.RunGame import RunGame
from src.stat.StatsAll import StatsAll


class RunAll:

    def __init__(self):

        self.num_games = 1000
        self.summary_at_step = 100
        self.decisions = Decisions()
        self.stats = StatsAll()

    def run(self):

        for count in range(self.num_games):

            game = Game()
            engine = RunGame(game, self.decisions)

            while not engine.end_of_game:
                engine.run_bout()

            game.apply_final()
            self.stats.incorporate(game.stats)



