# src.data
from enum import Enum, auto
from typing import List
from src.data.Player import Player
from src.data.Deck import Deck


class TurnPhase(Enum):
    NONE = 0
    DEAL = auto()
    PLACEMENT = auto()
    REVEAL_INTENTION_MAX = auto()
    ATTACK_DECLARE = auto()
    ATTACK_INTENTION_SUPPORT = auto()
    DEFEND_INTENTION_REVEAL = auto()
    COMBAT_PRE_ROLL_HANDLING = auto()
    COMBAT_ROLL = auto()
    COMBAT_POST_ROLL_HANDLING = auto()
    DEPLOY_INTENTION_SUPPORT = auto()
    DEPLOY_PRE_ROLL_HANDLING = auto()
    DEPLOY_ROLL = auto()
    DEPLOY_POST_ROLL_HANDLING = auto()
    DEPLOY_ACQUIRE = auto()
    DEPLOY_BLOCKING = auto()
    CLEANUP = 6


class InitiativeOn(Enum):
    NONE = 0
    PLAYER_1 = 1
    PLAYER_2 = 2


class Game:

    def __init__(self):
        self.turnPhase = TurnPhase.NONE
        self.initiativeOn = InitiativeOn.NONE
        self.agentPlayer = Player()
        self.opponent = Player()
        self.commonDrawDeck = Deck()

    def nn_next_cards(self, size: int) -> List[int]:
        return self.agentPlayer.nn_next_cards(size)

    def agent_energy(self) -> int:
        return self.agentPlayer.energy

    def agent_pips(self) -> int:
        return self.agentPlayer.pips

    def agent_stash_cards_total(self) -> int:
        return self.agentPlayer.stash_cards_total

