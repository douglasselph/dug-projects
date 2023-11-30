from typing import Tuple
from src.data.Decision import DecisionLine, DecisionIntention
from src.data.Game import Game


class BasePlaceCard:

    def forAgent(self, game: Game) -> Tuple[DecisionLine, DecisionIntention]:
        return DecisionLine.LINE_1, DecisionIntention.DEPLOY

    def forOpponent(self, game: Game) -> Tuple[DecisionLine, DecisionIntention]:
        return DecisionLine.LINE_1, DecisionIntention.DEPLOY