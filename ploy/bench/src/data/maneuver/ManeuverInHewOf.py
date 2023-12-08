from src.data.die.DieValues import DieValues


# Reroll all one’s rolled.
class ManeuverInHewOf:

    @staticmethod
    def apply(values: DieValues):
        for die in values.values:
            if die.value == 1:
                die.roll()
