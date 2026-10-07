# Learned Mulch Target Policy

Mulch targeting is an independent policy family. Cultivation Main decides whether to select the Round-card Mulch effect; Mulch Target decides which legal Hand die is stored after that choice.

Human mode delegates exactly to the existing Human Baseline EffectStrategy / MulchPriority logic. Learned mode scores every legal die candidate with one shared linear feature function and selects the maximum. The number of dice in Hand therefore changes the number of candidates, not the model shape; there is no first-yes or iteration-order decision.

The feature vector exposes the candidate face, sides, face fraction, expected reroll value, reroll gain, headroom, relative face/side position in Hand, Hand totals, purchasing-tier loss caused by removing the die, Battle timing, existing stored Mulch summaries, and Human-reference agreement.

Train only this family:

    bin/train_mulch_target_policy --players 4 --rounds 3/2/2

Evaluate held-out against Human Mulch targeting on a matched schedule:

    bin/evaluate_mulch_target_policy --players 4 --rounds 3/2/2 --weights output/ai/mulch-target-policy-v1-trained.weights

The default trainer/evaluator uses the current 4p Plant and Round resync files. Future player-count-specific champions should be stored under data/ai/2p, data/ai/3p, or data/ai/4p and must record trainingPlayerCount in the weight file.
