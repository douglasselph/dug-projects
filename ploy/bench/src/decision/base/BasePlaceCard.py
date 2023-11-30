from typing import Tuple
from src.data.Decision import DecisionLine, DecisionIntention
from src.data.Game import Game


class BasePlaceCard:

    def prepare(self, game: Game):
        pass

    def decision_agent(self) -> Tuple[DecisionLine, DecisionIntention]:
        return DecisionLine.LINE_1, DecisionIntention.DEPLOY

    def result_agent(self, line: DecisionLine, coin: DecisionIntention, legal: bool):
        pass

    def decision_opponent(self) -> Tuple[DecisionLine, DecisionIntention]:
        return DecisionLine.LINE_1, DecisionIntention.DEPLOY

    def result_opponent(self, line: DecisionLine, coin: DecisionIntention, legal: bool):
        pass
