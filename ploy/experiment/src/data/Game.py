from src.data.Moves import Moves
from src.data.Player import Player
from src.stat.StatsGame import StatsGame


class Game:

    def __init__(self):
        self.player1 = Player()
        self.player2 = Player()
        self.stats = StatsGame()
        self.moves = Moves()

    @property
    def all_players_are_alive(self) -> bool:
        return self.player1.is_alive and self.player2.is_alive

    def add(self, moves: Moves):
        self.stats.num_bouts += 1
        moves.set_bout(self.stats.num_bouts)
        self.moves.extend(moves)

    def apply_final(self):
        self.stats.player1_finalHP = self.player1.hp
        self.stats.player2_finalHP = self.player2.hp



