from __future__ import annotations

from typing import Tuple, Optional, List
from src.data.Decision import DecisionLine, DecisionIntention
from src.decision.base.BasePlaceCard import BasePlaceCard


class TestPlaceCard(BasePlaceCard):

    def __init__(self):
        super().__init__()
        self.agent_result_line: List[DecisionLine] = []
        self.agent_result_coin: List[DecisionIntention] = []
        self.agent_got_line: List[DecisionLine] = []
        self.agent_got_coin: List[DecisionIntention] = []
        self.agent_got_legal: List[bool] = []
        self.opponent_result_line: List[DecisionLine] = []
        self.opponent_result_coin: List[DecisionIntention] = []
        self.opponent_got_line: List[DecisionLine] = []
        self.opponent_got_coin: List[DecisionIntention] = []
        self.opponent_got_legal: List[bool] = []

    def decision_agent(self) -> Tuple[DecisionLine, DecisionIntention]:
        counter = len(self.agent_got_line)
        if counter < len(self.agent_result_line):
            line = self.agent_result_line[counter]
        else:
            line = DecisionLine.LINE_1
        if counter < len(self.agent_result_coin):
            coin = self.agent_result_coin[counter]
        else:
            coin = DecisionIntention.NONE
        return line, coin

    def result_agent(self, line: DecisionLine, coin: DecisionIntention, legal: bool):
        self.agent_got_line.append(line)
        self.agent_got_coin.append(coin)
        self.agent_got_legal.append(legal)

    def decision_opponent(self) -> Tuple[DecisionLine, DecisionIntention]:
        counter = len(self.opponent_got_line)
        if counter < len(self.opponent_result_line):
            line = self.opponent_result_line[counter]
        else:
            line = DecisionLine.LINE_1
        if counter < len(self.opponent_result_coin):
            coin = self.opponent_result_coin[counter]
        else:
            coin = DecisionIntention.NONE
        return line, coin

    def result_opponent(self, line: DecisionLine, coin: DecisionIntention, legal: bool):
        self.opponent_got_line.append(line)
        self.opponent_got_coin.append(coin)
        self.opponent_got_legal.append(legal)
