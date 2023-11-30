from src.engine.Engine import Engine
from src.data.Game import Game
from src.data.Stats import StatsAll


class RunEngine:

    def __init__(self, times: int, step: int):
        self.times = times
        self.summary_at_step = step
        self.stats = StatsAll()

    def run(self):

        for game_count in range(self.times):
            game = Game()
            engine = Engine(game)

            while not game.endOfGame:
                game.stat.turns += 1
                engine.draw_hands()
                # Place Cards
                engine.reveal_intentions()
                game.stat.add(engine.resolve_attacks())
                game.stat.add2(engine.resolve_deploy())
                engine.cleanup()

            self.stats.apply(game.stat)
