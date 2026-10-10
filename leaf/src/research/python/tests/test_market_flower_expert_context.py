from pathlib import Path
from leaf_experiments.market.flower_expert_context import Config, train_cmd, eval_cmd

def cfg(tmp_path: Path) -> Config:
    root=tmp_path
    return Config(root, Path('cert'), Path('out'), replicates=3, cycles=3)

def test_cycle_one_bootstraps_from_certified_buy_and_zero_then_trains_fresh_buy(tmp_path):
    c=cfg(tmp_path)
    cmd=train_cmd(c,2,1,1,'buy')
    assert str(c.certified_root/'2p/weights/learner-1.weights') not in cmd
    assert str(c.output_root/'config/START_ZERO_buy.weights') in cmd
    assert '--cultivation-main-policy' in cmd and '--plant-effect-policy' in cmd and '--battle-support-policy' in cmd

def test_cycle_two_warm_starts_buy_and_uses_latest_context(tmp_path):
    c=cfg(tmp_path)
    cmd=train_cmd(c,4,2,2,'buy')
    assert str(c.output_root/'4p/replicate-2/cycle-1/weights/buy.weights') in cmd
    assert str(c.output_root/'4p/replicate-2/cycle-2/weights/cultivation-main.weights') in cmd
    assert str(c.output_root/'4p/replicate-2/cycle-2/weights/plant-effect.weights') in cmd
    assert str(c.output_root/'4p/replicate-2/cycle-2/weights/battle-support.weights') in cmd

def test_eval_uses_all_three_learned_companions_and_final_sample_size(tmp_path):
    c=cfg(tmp_path)
    cmd=eval_cmd(c,2,3,3)
    assert cmd[cmd.index('--games')+1] == '4000'
    assert cmd.count('learned') == 3
    assert '--market-json' in cmd


def test_buy_training_command_passes_explicit_round_pattern(tmp_path):
    c=cfg(tmp_path)
    cmd=train_cmd(c,2,1,1,'buy')
    assert '--rounds' in cmd
    assert cmd[cmd.index('--rounds')+1] == '3/2/2'


def test_all_overnight_training_commands_use_same_round_pattern(tmp_path):
    c=cfg(tmp_path)
    for family in ('cultivation-main','plant-effect','battle-support','buy'):
        cmd=train_cmd(c,4,1,1,family)
        assert cmd[cmd.index('--rounds')+1] == c.rounds
