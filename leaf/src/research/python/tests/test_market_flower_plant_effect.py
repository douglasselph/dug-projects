from pathlib import Path

from leaf_experiments.market.flower_plant_effect import Config, build_eval, build_train, seeds


def _config(tmp_path: Path) -> Config:
    return Config(
        project_root=tmp_path,
        certified_root=Path("certified"),
        output_root=Path("out"),
        player_counts=(2, 4),
        learners=5,
    )


def test_plant_effect_training_starts_zero_and_uses_certified_buy(tmp_path: Path) -> None:
    c = _config(tmp_path)
    cmd = build_train(c, 2, 3)
    joined = " ".join(cmd)
    assert "START_FROM_ZERO_PLANT_EFFECT.weights" in joined
    assert "--buy-policy learned" in joined
    assert str(tmp_path / "certified/2p/weights/learner-3.weights") in joined
    assert "--random-grove" in cmd


def test_matched_context_evaluations_share_seed_schedule(tmp_path: Path) -> None:
    c = _config(tmp_path)
    human = build_eval(c, 4, 2, "human-plant-effect")
    learned = build_eval(c, 4, 2, "learned-plant-effect")
    s = seeds(4, 2)
    for flag, expected in (
        ("--seed", str(s["eval_mech"])),
        ("--strategy-seed", str(s["eval_strat"])),
        ("--grove-seed", str(s["eval_grove"])),
    ):
        hi = human.index(flag)
        li = learned.index(flag)
        assert human[hi + 1] == expected
        assert learned[li + 1] == expected
    assert human[human.index("--plant-effect-policy") + 1] == "human"
    assert learned[learned.index("--plant-effect-policy") + 1] == "learned"
    assert "--plant-effect-weights" in learned
