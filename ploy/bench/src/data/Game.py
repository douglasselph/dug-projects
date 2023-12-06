# src.data
from enum import Enum, auto
from typing import List
from src.data.Player import Player
from src.data.Deck import Deck
from src.data.Decision import DecisionIntention, DecisionLine
from src.data.Stats import StatsGame, StatsAll
from src.data.Card import CardComposite


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
    _scale_reward = 20

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
        return self.agentPlayer.line_intention_id(line)

    def agent_line_card_values(self, line: DecisionLine) -> List[int]:
        return self.agentPlayer.line_card_values(line)

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

    def is_legal_on_agent_plate(self, line: DecisionLine, intention: DecisionIntention):
        return self.agentPlayer.is_legal(line, intention)

    @property
    def common_cards_face_up(self) -> List[CardComposite]:
        return self.commonDrawDeck.face_up_deck

    @property
    def common_cards_draw(self) -> List[CardComposite]:
        return self.commonDrawDeck.draw_deck

    def common_pull_face_up_card(self) -> CardComposite:
        return self.commonDrawDeck.pull_face_up_card()

    @property
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
        agent_energy_loss = self.agentPlayer.nn_energy_loss
        opponent_energy_loss = self.opponent.nn_energy_loss
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

    def apply_stats(self, all: StatsAll):
        all.games += 1
        all.total_turns = self.stat.turns

        if self.stat.turns > all.highest_turns:
            all.highest_turns = self.stat.turns
        if all.lowest_turns == 0 or all.lowest_turns < self.stat.turns:
            all.lowest_turns = self.stat.turns

        if self.agentPlayer.fatal_received:
            if self.opponent.fatal_received:
                all.ties += 1
            else:
                all.fatal_loss += 1
        elif self.opponent.fatal_received:
            all.fatal_wins += 1
        elif self.agentPlayer.energy <= 0:
            if self.opponent.energy <= 0:
                all.ties += 1
            else:
                all.energy_loss += 1
        elif self.opponent.energy <= 0:
            all.energy_wins += 1

        all.total_num_attacks += self.stat.num_attacks
        all.total_num_defends += self.stat.num_defends
        all.total_num_deploys += self.stat.num_deploys
        all.total_attack_roll += self.stat.total_attack_roll
        all.total_defend_roll += self.stat.total_defend_roll
        all.total_deploy_roll += self.stat.total_deploy_roll
        all.total_draw_deck_size_agent += self.agentPlayer.num_cards_draw
        all.total_draw_deck_size_opponent += self.opponent.num_cards_draw
        all.total_agent_energy_lost += self.agentPlayer.energy_loss
        all.total_opponent_energy_lost += self.opponent.energy_loss

        for wound in self.stat.agent_wounds:
            if wound not in all.total_agent_wounds:
                all.total_agent_wounds[wound] = 0
            all.total_agent_wounds[wound] += 1
        for wound in self.stat.opponent_wounds:
            if wound not in all.total_opponent_wounds:
                all.total_opponent_wounds[wound] = 0
            all.total_opponent_wounds[wound] += 1

        if self.stat.highest_attack_roll > all.highest_attack_roll:
            all.highest_attack_roll = self.stat.highest_attack_roll
        if self.stat.highest_defend_roll > all.highest_defend_roll:
            all.highest_defend_roll = self.stat.highest_defend_roll
        if self.stat.highest_deploy_roll > all.highest_deploy_roll:
            all.highest_deploy_roll = self.stat.highest_deploy_roll

        if self.stat.lowest_attack_roll < all.lowest_attack_roll:
            all.lowest_attack_roll = self.stat.lowest_attack_roll
        if self.stat.lowest_defend_roll < all.lowest_defend_roll:
            all.lowest_defend_roll = self.stat.lowest_defend_roll
        if self.stat.lowest_deploy_roll < all.lowest_deploy_roll:
            all.lowest_deploy_roll = self.stat.lowest_deploy_roll
