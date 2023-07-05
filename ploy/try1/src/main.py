from src.build import build_deck
from src.faction import Faction
from src.game import Game


for faction in Faction.__members__.values():
    game = Game(build_deck(faction))
    result = game.rounds(1000)
    print(f"{faction}:")
    result.show()
