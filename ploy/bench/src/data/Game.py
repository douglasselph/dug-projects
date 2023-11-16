# src.data
from enum import Enum, auto
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


class Game:

    def __init__(self):
        self.agentPlayer = Player()
        self.opponent = Player()
        self.turnPhase = TurnPhase.NONE
        self.commonDrawDeck = Deck()

