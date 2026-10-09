"""Re-evaluate certified Buy learners to measure what they choose instead of legal Plants.

This is an evaluation-only diagnostic. It reuses already-trained learner weights from a
certified current-market confirmation, emits schema-v3 structured market JSON, and writes
the normal market reports plus rejected-legal substitution reports.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass
import os
from pathlib import Path
import shutil
import sys
from typing import Sequence

from leaf_experiments.paths import find_project_root
from leaf_experiments.timing import ExperimentTimer

from .confirmation import (
    ConfirmationConfig,
    CONFIRMATION_PROFILE,
    create_archive,
    default_process_executor,
    seed_plan,
)
from .reader import load_market_result
from .reports import write_market_reports


@dataclass(frozen=True)
class SubstitutionConfig:
    project_root: Path
    source_root: Path
    output_root: Path
    player_counts: tuple[int, ...] = (2, 3, 4)
    learners: int = 5
    eval_games: int = 2500
    rounds: str = "3/2/2"
    plant_overrides: Path | None = None
    round_overrides: Path | None = None
    resume: bool = True

    def __post_init__(self) -> None:
        root = self.project_root.resolve()
        object.__setattr__(self, "project_root", root)
        for field in ("source_root", "output_root"):
            value = getattr(self, field)
            object.__setattr__(self, field, value if value.is_absolute() else (root / value))
        if self.plant_overrides is None:
            object.__setattr__(self, "plant_overrides", root / "data/research/4p/resync/resync-current.csv")
        elif not self.plant_overrides.is_absolute():
            object.__setattr__(self, "plant_overrides", root / self.plant_overrides)
        if self.round_overrides is None:
            object.__setattr__(self, "round_overrides", root / "data/research/4p/resync/round-resync-current.csv")
        elif not self.round_overrides.is_absolute():
            object.__setattr__(self, "round_overrides", root / self.round_overrides)


def source_weight_path(config: SubstitutionConfig, players: int, learner: int) -> Path:
    return config.source_root / f"{players}p/weights/learner-{learner}.weights"


def raw_json_path(config: SubstitutionConfig, players: int, learner: int) -> Path:
    return config.output_root / f"{players}p/eval/learner-{learner}.market.json"


def eval_log_path(config: SubstitutionConfig, players: int, learner: int) -> Path:
    return config.output_root / f"{players}p/eval/learner-{learner}.log"


def complete_path(config: SubstitutionConfig, players: int, learner: int) -> Path:
    return Path(str(eval_log_path(config, players, learner)) + ".complete")


def _seed_config(config: SubstitutionConfig) -> ConfirmationConfig:
    # Only seed allocation is reused; no training is performed by this diagnostic.
    return ConfirmationConfig(
        project_root=config.project_root,
        profile=CONFIRMATION_PROFILE,
        player_counts=config.player_counts,
        learners=config.learners,
        eval_games=config.eval_games,
        rounds=config.rounds,
        plant_overrides=config.plant_overrides,
        round_overrides=config.round_overrides,
        output_root=config.output_root,
    )


def build_eval_command(config: SubstitutionConfig, players: int, learner: int) -> list[str]:
    seeds = seed_plan(_seed_config(config), players, learner)
    return [
        str(config.project_root / "bin/evaluate_buy_policy"),
        "--players", str(players),
        "--weights", str(source_weight_path(config, players, learner)),
        "--games", str(config.eval_games),
        "--seed", str(seeds.eval_mechanical_seed),
        "--strategy-seed", str(seeds.eval_strategy_seed),
        "--random-grove",
        "--grove-seed", str(seeds.eval_grove_seed),
        "--rounds", config.rounds,
        "--plant-overrides", str(config.plant_overrides),
        "--round-overrides", str(config.round_overrides),
        "--market-json", str(raw_json_path(config, players, learner)),
    ]


def validate_preflight(config: SubstitutionConfig) -> None:
    evaluator = config.project_root / "bin/evaluate_buy_policy"
    if not evaluator.is_file() or not os.access(evaluator, os.X_OK):
        raise FileNotFoundError(f"missing executable: {evaluator}")
    for path in (config.plant_overrides, config.round_overrides):
        if not path.is_file():
            raise FileNotFoundError(f"missing baseline: {path}")
    missing = [
        source_weight_path(config, p, l)
        for p in config.player_counts
        for l in range(1, config.learners + 1)
        if not source_weight_path(config, p, l).is_file()
    ]
    if missing:
        raise FileNotFoundError("missing certified learner weights:\n" + "\n".join(map(str, missing)))


def _is_complete(config: SubstitutionConfig, players: int, learner: int) -> bool:
    if not config.resume:
        return False
    marker = complete_path(config, players, learner)
    raw = raw_json_path(config, players, learner)
    if not marker.is_file() or not raw.is_file():
        return False
    try:
        return load_market_result(raw).schema_version >= 3
    except Exception:
        return False


def _write_config(config: SubstitutionConfig) -> None:
    cfg = config.output_root / "config"
    cfg.mkdir(parents=True, exist_ok=True)
    shutil.copy2(config.plant_overrides, cfg / "plant-resync-current.csv")
    shutil.copy2(config.round_overrides, cfg / "round-resync-current.csv")
    (cfg / "experiment.txt").write_text(
        "experiment=current-market-substitution-diagnostic\n"
        f"sourceCertifiedRoot={config.source_root}\n"
        f"players={' '.join(map(str, config.player_counts))}\n"
        f"learnersPerPlayerCount={config.learners}\n"
        f"heldOutMatchedSamples={config.eval_games}\n"
        f"rounds={config.rounds}\n"
        "purpose=measure actual alternatives selected when each Plant was legal but rejected\n"
        "training=none; reuse certified learner weights\n"
        "reporting=structured Kotlin JSON schema v3 -> validated Python reports\n",
        encoding="utf-8",
    )


def run_substitution_diagnostic(config: SubstitutionConfig, *, executor=default_process_executor):
    validate_preflight(config)
    config.output_root.mkdir(parents=True, exist_ok=True)
    _write_config(config)

    plans = [
        (f"eval-{p}p", _is_complete(config, p, l))
        for p in config.player_counts for l in range(1, config.learners + 1)
    ]
    timer = ExperimentTimer(tuple(x for x, _ in plans), planned_skips=tuple(y for _, y in plans))
    results = []

    print("================================================================", flush=True)
    print("CURRENT MARKET SUBSTITUTION DIAGNOSTIC", flush=True)
    print("================================================================", flush=True)
    print(f"Source weights: {config.source_root}", flush=True)
    print(f"Output:         {config.output_root}", flush=True)
    print("Training:       NONE — certified learner weights reused", flush=True)
    print("Question:       when a Plant is legal but rejected, what is chosen instead?", flush=True)
    print("================================================================", flush=True)

    for players in config.player_counts:
        for learner in range(1, config.learners + 1):
            label = f"Evaluate substitutions {players}p learner {learner}/{config.learners}"
            timer.start(label, f"eval-{players}p")
            raw = raw_json_path(config, players, learner)
            log = eval_log_path(config, players, learner)
            marker = complete_path(config, players, learner)
            raw.parent.mkdir(parents=True, exist_ok=True)
            skipped = _is_complete(config, players, learner)
            if skipped:
                print("Already complete; schema-v3 structured result is present.", flush=True)
            else:
                marker.unlink(missing_ok=True)
                raw.unlink(missing_ok=True)
                executor(build_eval_command(config, players, learner), log, config.project_root)
                result = load_market_result(raw)
                if result.schema_version < 3:
                    raise RuntimeError(f"expected schema v3 substitution telemetry: {raw}")
                marker.touch()
            timer.complete(skipped=skipped)
            results.append(load_market_result(raw))

    reports = config.output_root / "reports"
    write_market_reports(tuple(results), reports)
    archive = config.output_root / "current-market-substitution-diagnostic-results.tar.gz"
    create_archive(config.output_root, archive)
    timer.finish("CURRENT MARKET SUBSTITUTION DIAGNOSTIC complete")
    print(f"Substitution summary:   {reports / 'card-substitution-summary.tsv'}", flush=True)
    print(f"By learner:             {reports / 'card-substitutions-by-learner.tsv'}", flush=True)
    print(f"Opportunity summary:    {reports / 'card-opportunity-summary.tsv'}", flush=True)
    print(f"Archive:                {archive}", flush=True)
    return tuple(results)


def main(argv: Sequence[str] | None = None) -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(line_buffering=True)
    root = find_project_root()
    parser = argparse.ArgumentParser(description="Measure what certified Buy learners select instead of legal Plants.")
    parser.add_argument("--source-root", type=Path, default=Path("output/experiments/current-market-confirmation-certified"))
    parser.add_argument("--output-root", type=Path, default=Path("output/experiments/current-market-substitution-diagnostic"))
    parser.add_argument("--players", nargs="+", type=int, choices=(2, 3, 4), default=[2, 3, 4])
    parser.add_argument("--learners", type=int, default=5)
    parser.add_argument("--eval-games", type=int, default=2500)
    parser.add_argument("--resume", choices=("0", "1"), default="1")
    args = parser.parse_args(argv)
    config = SubstitutionConfig(
        project_root=root,
        source_root=args.source_root,
        output_root=args.output_root,
        player_counts=tuple(args.players),
        learners=args.learners,
        eval_games=args.eval_games,
        resume=args.resume == "1",
    )
    run_substitution_diagnostic(config)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
