# src.data
from enum import Enum, auto
from typing import List, Optional
from src.data.Player import Player
from src.data.Deck import Deck
from src.data.ManeuverPlate import IntentionID
from src.data.Decision import DecisionIntention, DecisionLine
from src.data.GameStat import GameStat

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

    _scale_reward = 10

    def __init__(self):
        self.turnPhase = TurnPhase.NONE
        self.initiativeOn = InitiativeOn.NONE
        self.agentPlayer = Player()
        self.opponent = Player()
        self.commonDrawDeck = Deck()
        self.stat = GameStat()

    def nn_next_cards(self, size: int) -> List[int]:
        return self.agentPlayer.nn_next_cards(size)

    @property
    def agent_energy(self) -> int:
        return self.agentPlayer.energy

    @property
    def agent_pips(self) -> int:
        return self.agentPlayer.pips

    @property
    def agent_stash_cards_total(self) -> int:
        return self.agentPlayer.stash_cards_total

    @property
    def agent_line_sizes(self) -> List[int]:
        return self.agentPlayer.line_sizes

    def agent_line_intention_id(self, line_position: int) -> IntentionID:
        return self.agentPlayer.line_intention_id(line_position)

    def agent_line_card_values(self, line_position: int) -> List[int]:
        return self.agentPlayer.line_card_values(line_position)

    @property
    def opponent_energy(self) -> int:
        return self.opponent.energy

    @property
    def opponent_pips(self) -> int:
        return self.opponent.pips

    @property
    def opponent_lines_num_cards(self) -> List[int]:
        return self.opponent.lines_num_cards

    def nn_common_draw_deck_face_up_cards(self, size: int) -> List[int]:
        return self.commonDrawDeck.nn_face_up_cards(size)

    def is_legal(self, line: DecisionLine, intention: DecisionIntention):
        return True

    def compute_reward(self) -> int:
        base = 0
        if self.agentPlayer.fatal_received:
            if self.opponent.fatal_received:
                # Tie
                base += 0
            else:
                base -= 100
        elif self.agentPlayer.energy <= 0:
            if self.opponent.fatal_received:
                # Won, but just barely.
                base += 50
            elif self.opponent.energy <= 0:
                # Tie
                base += 0
            else:
                # Loss
                base -= 100
        else:
            if self.opponent.fatal_received:
                # Won
                base += 100
            elif self.opponent.energy <= 0:
                # Won
                base += 100

        self.agentPlayer.discard_all()
        self.opponent.discard_all()

        agent_wounds = self.agentPlayer.nn_wound_value
        opponent_wounds = self.opponent.nn_wound_value
        agent_energy_loss = self.agentPlayer.energy_loss
        opponent_energy_loss = self.opponent.energy_loss
        num_turns = self.stat.num_turns

        if base > 0:
            # Core Win
            base_reward = base - agent_wounds - agent_energy_loss
        elif base < 0:
            # Core Loss
            base_reward = base + opponent_wounds + opponent_energy_loss
        else:
            # Tie
            base_reward = opponent_wounds + opponent_energy_loss - agent_wounds - agent_energy_loss

        return base_reward / num_turns * self._scale_reward


