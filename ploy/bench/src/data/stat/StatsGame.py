from typing import Optional, List
from src.data.Card import CardWound
from src.data.stat.StatsAttack import StatsAttack, StatsDeploy


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

