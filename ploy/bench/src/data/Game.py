# package: src.data
from enum import Enum, auto
from typing import List
from src.data.Player import Player
from src.data.Deck import Deck
from src.data.Decision import DecisionIntention, DecisionLine
from src.data.stat.StatsGame import StatsGame
from src.data.Card import CardComposite
from src.data.RewardConstants import RewardConstants


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


class PlayerID(Enum):
    NONE = 0
    PLAYER_1 = 1
    PLAYER_2 = 2


class Game:
    def __init__(self):
        self.turnPhase = TurnPhase.NONE
        self.initiativeOn = PlayerID.NONE
        self.agentPlayer = Player()
        self.opponent = Player()
        self.commonDrawDeck = Deck()
        self.trash = Deck()
        self.stat = StatsGame()
        self.endOfGame = False

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

    def agent_line_intention_id(self, line: DecisionLine) -> DecisionIntention:
        return self.agentPlayer.line_intention_of(line)

    def agent_line_card_values(self, line: DecisionLine) -> List[int]:
        return self.agentPlayer.nn_line_card_values(line)

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

    def is_legal_intention_on_agent_plate(self, line: DecisionLine, intention: DecisionIntention):
        return self.agentPlayer.is_legal_intention(line, intention)

    @property
    def common_cards_face_up(self) -> List[CardComposite]:
        return self.commonDrawDeck.face_up_deck

    @property
    def common_cards_draw(self) -> List[CardComposite]:
        return self.commonDrawDeck.draw_deck

    def common_pull_face_up_card(self) -> CardComposite:
        return self.commonDrawDeck.pull_face_up_card()

    @property
    def compute_base_reward(self) -> int:
        base = 0
        if self.agentPlayer.fatal_received:
            if self.opponent.fatal_received:
                # Tie
                base += 0
            else:
                base += RewardConstants.BASE_PENALTY_LOSS
        elif self.agentPlayer.energy <= 0:
            if self.opponent.fatal_received:
                base += RewardConstants.BASE_REWARD_WIN - RewardConstants.EXHAUSTED_WIN_PENALTY
            elif self.opponent.energy <= 0:
                # Tie
                base += 0
            else:
                # Loss
                base += RewardConstants.BASE_PENALTY_LOSS
        else:
            if self.opponent.fatal_received:
                # Won
                base += RewardConstants.BASE_REWARD_WIN
            elif self.opponent.energy <= 0:
                # Won
                base += RewardConstants.BASE_REWARD_WIN

        return base

    @property
    def compute_wound_reward(self) -> int:

        self.agentPlayer.discard_all()
        self.opponent.discard_all()

        agent_wounds = self.agentPlayer.compute_draw_wound_penalty_value
        opponent_wounds = self.opponent.compute_draw_wound_penalty_value

        return opponent_wounds - agent_wounds

    @property
    def compute_energy_penalty(self) -> int:
        if self.agent_energy < RewardConstants.ENERGY_PENALTY_THRESHOLD:
            return RewardConstants.ENERGY_PENALTY_THRESHOLD - self.agent_energy
        return 0

    @property
    def turns(self) -> int:
        return self.stat.turns

    # Reward formula:
    #   BASE - ENERGY_PENALTY * ENERGY_SCALE - WOUND_PENALTY * WOUND_SCALE - TURNS * TURN_SCALE
    @property
    def compute_reward(self) -> int:
        return self.compute_base_reward - \
            self.compute_energy_penalty * RewardConstants.ENERGY_PENALTY_SCALE - \
            self.compute_wound_reward * RewardConstants.WOUND_PENALTY_SCALE - \
            self.turns * RewardConstants.TURNS_PENALTY_SCALE

    # TODO:
    def cleanup(self):
        self._upgrade_face_up_wounds()
        self._lose_energy_from_face_up_wounds()
        self.discard_face_up()
        self.endOfGame = \
            self.agentPlayer.fatal_received or \
            self.agentPlayer.energy <= 0 or \
            self.opponent.fatal_received or \
            self.opponent.energy <= 0

    # TODO: Unit support
    def _upgrade_face_up_wounds(self):
        self.agentPlayer.upgrade_face_up_wounds()
        self.opponent.upgrade_face_up_wounds()

    # TODO: Unit support
    def _lose_energy_from_face_up_wounds(self):
        self.agentPlayer.lose_energy_from_face_up_wounds()
        self.opponent.lose_energy_from_face_up_wounds()

    # TODO: Unit support
    def discard_face_up(self):
        self.agentPlayer.discard_face_up()
        self.opponent.discard_face_up()