from __future__ import annotations

from src.data.Game import Game


class StatsAll:

    def __init__(self):
        self.games: int = 0
        self.fatal_wins = 0
        self.energy_wins = 0
        self.fatal_loss = 0
        self.energy_loss = 0
        self.ties: int = 0
        self.total_num_attacks: int = 0
        self.total_num_defends: int = 0
        self.total_num_deploys: int = 0
        self.total_draw_deck_size_agent: int = 0
        self.total_draw_deck_size_opponent: int = 0
        self.total_attack_roll: int = 0
        self.total_defend_roll: int = 0
        self.total_deploy_roll: int = 0
        self.total_agent_wounds = {}
        self.total_opponent_wounds = {}
        self.total_agent_energy_lost: int = 0
        self.total_opponent_energy_lost: int = 0
        self.total_turns: int = 0
        self.highest_attack_roll: int = 0
        self.highest_defend_roll: int = 0
        self.highest_deploy_roll: int = 0
        self.highest_turns: int = 0
        self.lowest_turns: int = 0
        self.lowest_attack_roll: int = 0
        self.lowest_defend_roll: int = 0
        self.lowest_deploy_roll: int = 0

    def apply(self, game: Game):
        self.games += 1
        self.total_turns = game.stat.turns

        if game.stat.turns > self.highest_turns:
            self.highest_turns = game.stat.turns
        if self.lowest_turns == 0 or self.lowest_turns < game.stat.turns:
            self.lowest_turns = game.stat.turns

        if game.agentPlayer.fatal_received:
            if game.opponent.fatal_received:
                self.ties += 1
            else:
                self.fatal_loss += 1
        elif game.opponent.fatal_received:
            self.fatal_wins += 1
        elif game.agentPlayer.energy <= 0:
            if game.opponent.energy <= 0:
                self.ties += 1
            else:
                self.energy_loss += 1
        elif game.opponent.energy <= 0:
            self.energy_wins += 1

        self.total_num_attacks += game.stat.num_attacks
        self.total_num_defends += game.stat.num_defends
        self.total_num_deploys += game.stat.num_deploys
        self.total_attack_roll += game.stat.total_attack_roll
        self.total_defend_roll += game.stat.total_defend_roll
        self.total_deploy_roll += game.stat.total_deploy_roll
        self.total_draw_deck_size_agent += game.agentPlayer.num_all_draw_cards
        self.total_draw_deck_size_opponent += game.opponent.num_all_draw_cards
        self.total_agent_energy_lost += game.agentPlayer.energy_loss
        self.total_opponent_energy_lost += game.opponent.energy_loss

        for wound in game.stat.agent_wounds:
            if wound not in self.total_agent_wounds:
                self.total_agent_wounds[wound] = 0
            self.total_agent_wounds[wound] += 1
        for wound in game.stat.opponent_wounds:
            if wound not in self.total_opponent_wounds:
                self.total_opponent_wounds[wound] = 0
            self.total_opponent_wounds[wound] += 1

        if game.stat.highest_attack_roll > self.highest_attack_roll:
            self.highest_attack_roll = game.stat.highest_attack_roll
        if game.stat.highest_defend_roll > self.highest_defend_roll:
            self.highest_defend_roll = game.stat.highest_defend_roll
        if game.stat.highest_deploy_roll > self.highest_deploy_roll:
            self.highest_deploy_roll = game.stat.highest_deploy_roll

        if game.stat.lowest_attack_roll < self.lowest_attack_roll or self.lowest_attack_roll == 0:
            self.lowest_attack_roll = game.stat.lowest_attack_roll
        if game.stat.lowest_defend_roll < self.lowest_defend_roll or self.lowest_defend_roll == 0:
            self.lowest_defend_roll = game.stat.lowest_defend_roll
        if game.stat.lowest_deploy_roll < self.lowest_deploy_roll or self.lowest_deploy_roll == 0:
            self.lowest_deploy_roll = game.stat.lowest_deploy_roll
