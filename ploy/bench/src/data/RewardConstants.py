
#
# Reward formula:
#   BASE - ENERGY_PENALTY * ENERGY_SCALE - WOUND_PENALTY * WOUND_SCALE - TURNS * TURN_SCALE
class RewardConstants:

    BASE_PENALTY_LOSS = -1000
    BASE_REWARD_WIN = 1000

    EXHAUSTED_WIN_PENALTY = 200

    ENERGY_PENALTY_THRESHOLD = 6
    ENERGY_PENALTY_SCALE = 10

    WOUND_PENALTY_DIRE = 8
    WOUND_PENALTY_GRAVE = 4
    WOUND_PENALTY_ACUTE = 2
    WOUND_PENALTY_MINOR = 1

    WOUND_PENALTY_SCALE = 10

    TURNS_PENALTY_SCALE = 1
