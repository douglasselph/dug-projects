class Intentions:
    # Slash:
    bust_a_cut: bool  # Attack: Take higher of two rolls
    d4_bonus: bool  # Defend: If attacking as well, +d4.
    # Fang:
    discard_for_bonus: bool  # Attack: Discard a die for a new card
    stupify: bool  # Defend: Attacker takes lower of two rolls
    # Thorn:
    counter_blow: bool  # Attack: each matching awards d4
    feint: bool  # Defend: transfer card between intentions
    # Raven:
    attack_re_roll_on_1: bool  # Attack: if 1 rolled then re-roll.
    defense_re_roll_on_1: bool  # Defense: if 1 rolled then re-roll.
