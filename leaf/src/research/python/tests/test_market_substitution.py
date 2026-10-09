from pathlib import Path

import pytest

from leaf_experiments.market.substitution import (
    SubstitutionConfig,
    build_eval_command,
    source_weight_path,
    raw_json_path,
    validate_preflight,
)


def make_config(tmp_path: Path) -> SubstitutionConfig:
    root = tmp_path / "leaf"
    (root / "bin").mkdir(parents=True)
    evaluator = root / "bin/evaluate_buy_policy"
    evaluator.write_text("#!/bin/sh\n", encoding="utf-8")
    evaluator.chmod(0o755)
    plant = root / "data/research/4p/resync/resync-current.csv"
    round_ = root / "data/research/4p/resync/round-resync-current.csv"
    plant.parent.mkdir(parents=True)
    plant.write_text("plant\n", encoding="utf-8")
    round_.write_text("round\n", encoding="utf-8")
    source = root / "output/experiments/current-market-confirmation-certified"
    for players in (2, 3, 4):
        for learner in range(1, 6):
            path = source / f"{players}p/weights/learner-{learner}.weights"
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("weights\n", encoding="utf-8")
    return SubstitutionConfig(
        project_root=root,
        source_root=Path("output/experiments/current-market-confirmation-certified"),
        output_root=Path("output/experiments/current-market-substitution-diagnostic"),
    )


def test_substitution_eval_reuses_certified_weights_and_structured_json(tmp_path: Path) -> None:
    config = make_config(tmp_path)
    command = build_eval_command(config, 4, 3)
    assert command[0].endswith("bin/evaluate_buy_policy")
    assert command[command.index("--weights") + 1] == str(source_weight_path(config, 4, 3))
    assert command[command.index("--market-json") + 1] == str(raw_json_path(config, 4, 3))
    assert command[command.index("--games") + 1] == "2500"
    assert "train_buy_policy" not in " ".join(command)


def test_substitution_preflight_requires_all_certified_weights(tmp_path: Path) -> None:
    config = make_config(tmp_path)
    validate_preflight(config)
    source_weight_path(config, 3, 2).unlink()
    with pytest.raises(FileNotFoundError, match="missing certified learner weights"):
        validate_preflight(config)
