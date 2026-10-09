"""Flower 14/17 causal ablation: Human vs learned Plant Effect/Targeting context.

For each certified Buy learner at 2p and 4p, train Plant Effect/Targeting from
zero with that frozen Buy learner active. Then evaluate the same Buy learner on
matched held-out games under Human Plant Effect vs learned Plant Effect context.
The primary metric is Flower selection when legal.
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
from leaf_experiments.timing import ExperimentPlanRunner, ExperimentStep

from .confirmation import create_archive, default_process_executor
from .flower_ablation_reports import collect, write_reports
from .reader import MARKET_SCHEMA_VERSION, load_market_result


@dataclass(frozen=True)
class Config:
    project_root: Path
    certified_root: Path
    output_root: Path
    player_counts: tuple[int, ...] = (2, 4)
    learners: int = 5
    generations: int = 8
    population: int = 10
    train_games: int = 50
    eval_games: int = 2500
    elites: int = 2
    sigma: float = 0.25
    mutations: int = 8
    resume: bool = True
    rounds: str = "3/2/2"
    plant_overrides: Path | None = None
    round_overrides: Path | None = None

    def __post_init__(self) -> None:
        root = self.project_root.resolve()
        object.__setattr__(self, "project_root", root)
        for field in ("certified_root", "output_root"):
            value = getattr(self, field)
            object.__setattr__(self, field, value if value.is_absolute() else root / value)
        if self.plant_overrides is None:
            object.__setattr__(self, "plant_overrides", root / "data/research/4p/resync/resync-current.csv")
        elif not self.plant_overrides.is_absolute():
            object.__setattr__(self, "plant_overrides", root / self.plant_overrides)
        if self.round_overrides is None:
            object.__setattr__(self, "round_overrides", root / "data/research/4p/resync/round-resync-current.csv")
        elif not self.round_overrides.is_absolute():
            object.__setattr__(self, "round_overrides", root / self.round_overrides)
        if not self.player_counts or any(p not in (2, 3, 4) for p in self.player_counts):
            raise ValueError("player_counts must contain only 2, 3, and/or 4")


def buy_path(c: Config, players: int, learner: int) -> Path:
    return c.certified_root / f"{players}p/weights/learner-{learner}.weights"


def plant_weights_path(c: Config, players: int, learner: int) -> Path:
    return c.output_root / f"{players}p/weights/plant-effect/learner-{learner}.weights"


def train_log(c: Config, players: int, learner: int) -> Path:
    return c.output_root / f"{players}p/train/plant-effect/learner-{learner}.log"


def eval_json(c: Config, players: int, learner: int, context: str) -> Path:
    return c.output_root / f"{players}p/eval/{context}/learner-{learner}.market.json"


def eval_log(c: Config, players: int, learner: int, context: str) -> Path:
    return c.output_root / f"{players}p/eval/{context}/learner-{learner}.log"


def marker(path: Path) -> Path:
    return Path(str(path) + ".complete")


def train_complete(c: Config, players: int, learner: int) -> bool:
    return c.resume and marker(train_log(c, players, learner)).is_file() and plant_weights_path(c, players, learner).is_file()


def eval_complete(c: Config, players: int, learner: int, context: str) -> bool:
    raw = eval_json(c, players, learner, context)
    if not (c.resume and marker(eval_log(c, players, learner, context)).is_file() and raw.is_file()):
        return False
    try:
        return load_market_result(raw).schema_version == MARKET_SCHEMA_VERSION
    except Exception:
        return False


def seeds(players: int, learner: int) -> dict[str, int]:
    # Training differs by learner. Both evaluation contexts intentionally share
    # the exact same held-out mechanical/strategy/Grove schedules within player count.
    return {
        "evolution": 8_110_000 + players * 10_000 + learner * 101,
        "train_mech": 8_210_000 + players * 100_000 + learner * 1_000,
        "train_strat": 8_310_000 + players * 100_000 + learner * 1_000,
        "train_grove": 8_410_000 + players * 100_000 + learner * 1_000,
        "eval_mech": 8_510_000 + players * 100_000,
        "eval_strat": 8_610_000 + players * 100_000,
        "eval_grove": 8_710_000 + players * 100_000,
    }


def build_train(c: Config, players: int, learner: int) -> list[str]:
    s = seeds(players, learner)
    # Missing input is deliberate: Plant Effect trainer initializes zero weights.
    zero_input = c.output_root / "config/START_FROM_ZERO_PLANT_EFFECT.weights"
    return [
        str(c.project_root / "bin/train_plant_effect_policy"),
        "--players", str(players),
        "--generations", str(c.generations),
        "--population", str(c.population),
        "--games", str(c.train_games),
        "--elites", str(c.elites),
        "--sigma", str(c.sigma),
        "--mutations", str(c.mutations),
        "--evolution-seed", str(s["evolution"]),
        "--seed", str(s["train_mech"]),
        "--strategy-seed", str(s["train_strat"]),
        "--input", str(zero_input),
        "--output", str(plant_weights_path(c, players, learner)),
        "--random-grove",
        "--grove-seed", str(s["train_grove"]),
        "--rounds", c.rounds,
        "--plant-overrides", str(c.plant_overrides),
        "--round-overrides", str(c.round_overrides),
        "--buy-policy", "learned",
        "--buy-weights", str(buy_path(c, players, learner)),
    ]


def build_eval(c: Config, players: int, learner: int, context: str) -> list[str]:
    s = seeds(players, learner)
    command = [
        str(c.project_root / "bin/evaluate_buy_policy"),
        "--players", str(players),
        "--weights", str(buy_path(c, players, learner)),
        "--games", str(c.eval_games),
        "--seed", str(s["eval_mech"]),
        "--strategy-seed", str(s["eval_strat"]),
        "--random-grove",
        "--grove-seed", str(s["eval_grove"]),
        "--rounds", c.rounds,
        "--plant-overrides", str(c.plant_overrides),
        "--round-overrides", str(c.round_overrides),
        "--market-json", str(eval_json(c, players, learner, context)),
    ]
    if context == "learned-plant-effect":
        command += [
            "--plant-effect-policy", "learned",
            "--plant-effect-weights", str(plant_weights_path(c, players, learner)),
        ]
    else:
        command += ["--plant-effect-policy", "human"]
    return command


def _run(command: Sequence[str], log: Path, root: Path) -> None:
    default_process_executor(command, log, root)
    marker(log).touch()


def validate(c: Config) -> None:
    for rel in ("bin/train_plant_effect_policy", "bin/evaluate_buy_policy"):
        path = c.project_root / rel
        if not path.is_file() or not os.access(path, os.X_OK):
            raise FileNotFoundError(f"missing executable: {path}")
    for path in (c.plant_overrides, c.round_overrides):
        if not path.is_file():
            raise FileNotFoundError(f"missing baseline: {path}")
    missing = [
        buy_path(c, p, l)
        for p in c.player_counts
        for l in range(1, c.learners + 1)
        if not buy_path(c, p, l).is_file()
    ]
    if missing:
        raise FileNotFoundError("missing certified Buy learners:\n" + "\n".join(map(str, missing)))


def write_config(c: Config) -> None:
    cfg = c.output_root / "config"
    cfg.mkdir(parents=True, exist_ok=True)
    (cfg / "START_FROM_ZERO_PLANT_EFFECT.weights").unlink(missing_ok=True)
    shutil.copy2(c.plant_overrides, cfg / "plant-overrides.csv")
    shutil.copy2(c.round_overrides, cfg / "round-overrides.csv")
    (cfg / "experiment.txt").write_text(
        "experiment=Flower 14/17 Plant Effect/Targeting context ablation\n"
        f"players={' '.join(map(str, c.player_counts))}\n"
        f"learners={c.learners}\n"
        f"plantEffectTraining=generations:{c.generations} population:{c.population} gamesPerPolicy:{c.train_games}\n"
        f"evaluationMatchedSamples={c.eval_games}\n"
        f"certifiedBuyRoot={c.certified_root}\n"
        "plantEffectInitialization=ZERO\n"
        "primaryMetric=Flower selection when legal\n",
        encoding="utf-8",
    )


def run(c: Config) -> Path:
    validate(c)
    c.output_root.mkdir(parents=True, exist_ok=True)
    write_config(c)

    print("================================================================", flush=True)
    print("FLOWER 14/17 WHY — PLANT EFFECT/TARGETING ABLATION", flush=True)
    print("================================================================", flush=True)
    print("Certified Buy learners stay frozen.", flush=True)
    print("Plant Effect/Targeting learners start from ZERO and train with each Buy learner active.", flush=True)
    print("Timing/ETA is mandatory through leaf_experiments.timing.ExperimentPlanRunner.", flush=True)
    print("================================================================", flush=True)

    steps: list[ExperimentStep] = []
    for players in c.player_counts:
        for learner in range(1, c.learners + 1):
            tlog = train_log(c, players, learner)
            tout = plant_weights_path(c, players, learner)
            tlog.parent.mkdir(parents=True, exist_ok=True)
            tout.parent.mkdir(parents=True, exist_ok=True)
            steps.append(ExperimentStep(
                label=f"Train Plant Effect {players}p learner {learner}/{c.learners}",
                category=f"train-{players}p",
                skip=train_complete(c, players, learner),
                action=lambda p=players, l=learner, log=tlog: _run(build_train(c, p, l), log, c.project_root),
            ))
            for context in ("human-plant-effect", "learned-plant-effect"):
                elog = eval_log(c, players, learner, context)
                raw = eval_json(c, players, learner, context)
                elog.parent.mkdir(parents=True, exist_ok=True)
                raw.parent.mkdir(parents=True, exist_ok=True)
                steps.append(ExperimentStep(
                    label=f"Evaluate {context} {players}p learner {learner}/{c.learners}",
                    category=f"eval-{players}p",
                    skip=eval_complete(c, players, learner, context),
                    action=lambda p=players, l=learner, ctx=context, log=elog: _run(build_eval(c, p, l, ctx), log, c.project_root),
                ))

    ExperimentPlanRunner(steps).run(description="FLOWER PLANT EFFECT/TARGETING ABLATION complete")

    rows = collect(c.output_root, ("human-plant-effect", "learned-plant-effect"))
    if not rows:
        raise RuntimeError("No Flower market telemetry was produced")
    write_reports(
        c.output_root,
        rows,
        baseline_context="human-plant-effect",
        learned_context="learned-plant-effect",
        policy_label="Plant Effect/Targeting",
    )
    archive = c.output_root / "flower-tier-why-plant-effect-results.tar.gz"
    create_archive(c.output_root, archive)
    print(f"Reports: {c.output_root / 'reports'}", flush=True)
    print(f"Archive: {archive}", flush=True)
    return archive


def main(argv: Sequence[str] | None = None) -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(line_buffering=True)
    root = find_project_root()
    ap = argparse.ArgumentParser(description="Ablate Human vs learned Plant Effect/Targeting around certified Buy learners.")
    ap.add_argument("--certified-root", type=Path, default=Path("output/experiments/current-market-confirmation-certified"))
    ap.add_argument("--output-root", type=Path, default=Path("output/experiments/flower-tier-why-plant-effect"))
    ap.add_argument("--players", nargs="+", type=int, choices=(2, 3, 4), default=[2, 4])
    ap.add_argument("--learners", type=int, default=5)
    ap.add_argument("--generations", type=int, default=8)
    ap.add_argument("--population", type=int, default=10)
    ap.add_argument("--train-games", type=int, default=50)
    ap.add_argument("--eval-games", type=int, default=2500)
    ap.add_argument("--resume", choices=("0", "1"), default="1")
    a = ap.parse_args(argv)
    run(Config(
        project_root=root,
        certified_root=a.certified_root,
        output_root=a.output_root,
        player_counts=tuple(a.players),
        learners=a.learners,
        generations=a.generations,
        population=a.population,
        train_games=a.train_games,
        eval_games=a.eval_games,
        resume=a.resume == "1",
    ))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
