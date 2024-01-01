from typing import Optional
from src.data.Moves import Moves
from src.data.Player import Player
from src.stat.StatsGame import StatsGame
from src.data.ID import PlayerID


class Game:

    def __init__(self):
        self.player1 = Player()
        self.player2 = Player()
        self.stats = StatsGame()
        self.moves = Moves()
        self.bout_count = 0
        self.maneuvers_left = 0

    @property
    def all_players_are_alive(self) -> bool:
        return self.player1.is_alive and self.player2.is_alive

    def add(self, moves: Moves):
        self.bout_count += 1
        self.maneuvers_left -= 1
        self.stats.num_bouts += 1
        moves.set_bout(self.bout_count)
        self.moves.extend(moves)

    def apply_final(self):
        self.stats.player1_finalHP = self.player1.hp
        self.stats.player2_finalHP = self.player2.hp

    def get_player(self, pid: PlayerID) -> Optional[Player]:
        if pid == PlayerID.PLAYER_1:
            return self.player1
        elif pid == PlayerID.PLAYER_2:
            return self.player2
        return None

    def get_opponent(self, pid: PlayerID) -> Optional[Player]:
        if pid == PlayerID.PLAYER_2:
            return self.player1
        elif pid == PlayerID.PLAYER_1:
            return self.player2
        return None

