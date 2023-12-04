from __future__ import annotations

from typing import Optional, List
from src.data.Card import CardWound


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
        self.total_draw_deck_size_agent += game.agentPlayer.num_cards_draw
        self.total_draw_deck_size_opponent += game.opponent.num_cards_draw
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

        if game.stat.lowest_attack_roll < self.lowest_attack_roll:
            self.lowest_attack_roll = game.stat.lowest_attack_roll
        if game.stat.lowest_defend_roll < self.lowest_defend_roll:
            self.lowest_defend_roll = game.stat.lowest_defend_roll
        if game.stat.lowest_deploy_roll < self.lowest_deploy_roll:
            self.lowest_deploy_roll = game.stat.lowest_deploy_roll


class StatsGame:

    def __init__(self):

        self.turns: int = 0
        self.num_attacks: int = 0
        self.num_defends: int = 0
        self.num_deploys: int = 0
        self.total_attack_roll: int = 0
        self.total_defend_roll: int = 0
        self.total_deploy_roll: int = 0
        self.total_blocks: int = 0
        self.total_cards_purchased: int = 0
        self.agent_wounds: List[CardWound] = []
        self.agent_final_energy: int = 0
        self.agent_final_fatal = False
        self.opponent_wounds: List[CardWound] = []
        self.opponent_final_energy: int = 0
        self.opponent_final_fatal = False
        self.highest_attack_roll: int = 0
        self.highest_defend_roll: int = 0
        self.highest_deploy_roll: int = 0
        self.lowest_attack_roll: int = 0
        self.lowest_defend_roll: int = 0
        self.lowest_deploy_roll: int = 0

    def add(self, attack: StatsAttack):

        self.total_attack_roll += attack.agent_attack_roll + attack.opponent_attack_roll
        self.total_defend_roll += attack.agent_defend_roll + attack.opponent_defend_roll
        self.agent_wounds.append(attack.agent_wound)
        self.opponent_wounds.append(attack.opponent_wound)

        if attack.agent_attack_roll > 0:
            self.num_attacks += 1
            if attack.agent_attack_roll > self.highest_attack_roll:
                self.highest_attack_roll = attack.agent_attack_roll
        if attack.opponent_attack_roll > 0:
            self.num_attacks += 1
            if attack.opponent_attack_roll > self.highest_attack_roll:
                self.highest_attack_roll = attack.opponent_attack_roll
        if attack.agent_defend_roll > 0:
            self.num_defends += 1
            if attack.agent_defend_roll > self.highest_defend_roll:
                self.highest_defend_roll = attack.agent_defend_roll
        if attack.opponent_defend_roll > 0:
            self.num_defends += 1
            if attack.opponent_defend_roll > self.highest_defend_roll:
                self.highest_defend_roll = attack.opponent_defend_roll

    def add2(self, deploy: StatsDeploy):
        if deploy.agent_roll > 0:
            self.num_deploys += 1
            self.total_deploy_roll += deploy.agent_roll
        if deploy.opponent_roll > 0:
            self.num_deploys += 1
            self.total_deploy_roll += deploy.opponent_roll
        self.total_blocks += deploy.blocks
        self.total_cards_purchased += deploy.cards


class StatsAttack:

    def __init__(self):
        self.agent_attack_roll = 0
        self.agent_defend_roll = 0
        self.agent_wound: Optional[CardWound] = None
        self.opponent_attack_roll = 0
        self.opponent_defend_roll = 0
        self.opponent_wound: Optional[CardWound] = None

    @property
    def did_attack(self) -> bool:
        return self.agent_attack_roll != 0 or self.opponent_attack_roll != 0

    def add(self, agent: StatsAttack, opponent: StatsAttack) -> StatsAttack:
        self.agent_attack_roll = agent.agent_attack_roll
        self.agent_defend_roll = opponent.agent_defend_roll
        self.agent_wound = opponent.agent_wound

        self.opponent_attack_roll = opponent.agent_attack_roll
        self.opponent_defend_roll = agent.agent_defend_roll
        self.opponent_wound = agent.agent_wound

        return self

    def set(self, attacker_roll: int, defender_roll: int, wound: Optional[CardWound]):
        self.agent_attack_roll = attacker_roll
        self.agent_defend_roll = defender_roll
        self.agent_wound = wound


class StatsDeploy:

    def __init__(self):
        self.agent_roll = 0
        self.opponent_roll = 0
        self.blocks = 0
        self.cards = 0
