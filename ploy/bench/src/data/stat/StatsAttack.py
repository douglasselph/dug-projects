from __future__ import annotations
from typing import Optional
from src.data.Card import CardWound


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
