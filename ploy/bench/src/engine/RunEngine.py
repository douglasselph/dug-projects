from src.engine.Engine import Engine
from src.data.Game import Game
from src.data.stat.StatsAll import StatsAll
from src.decision.Decisions import Decisions


class RunEngineParams:
    times: int
    summary_at_step: int
    decisions: Decisions

    def __init__(self):
        self.times = 1000
        self.summary_at_step = 100
        self.decisions = Decisions()


class RunEngine:

    def __init__(self, params: RunEngineParams):
        self.params = params
        self.stats = StatsAll()

    def run(self):

        for game_count in range(self.params.times):
            game = Game()
            engine = Engine(game, self.params.decisions)

            while not game.endOfGame:
                game.stat.turns += 1
                engine.draw_hands()
                engine.place_cards()
                engine.reveal_intentions()
                game.stat.add(engine.resolve_attacks())
                game.stat.add2(engine.resolve_deploy())
                engine.cleanup()

            game.apply_to_all_stats(self.stats)
