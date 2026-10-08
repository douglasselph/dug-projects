"""Current-market confirmation experiment orchestration.

This module owns experiment configuration, deterministic seed allocation,
training/evaluation subprocess invocation, resume markers, structured JSON
loading/validation, tested report generation, timing/progress, and archival.
It intentionally never parses human-readable Kotlin console output for market
research data.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from datetime import datetime, timedelta
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import tarfile
import tempfile
import time
from typing import Callable, Iterable, Mapping, Sequence

from leaf_experiments.paths import find_project_root

from .models import MarketRawResult
from .reader import load_market_result
from .reports import write_market_reports


DEFAULT_PLAYER_COUNTS = (2, 3, 4)


@dataclass(frozen=True)
class MarketExperimentProfile:
    """Named scale/output profile for the shared current-market experiment runner."""

    name: str
    description: str
    banner: str
    learners: int
    generations: int
    population: int
    train_games: int
    eval_games: int
    output_directory: str
    archive_name: str


CONFIRMATION_PROFILE = MarketExperimentProfile(
    name="current-market-confirmation",
    description="CURRENT MARKET LONG CONFIRMATION — fresh Buy-only learners across 2p/3p/4p",
    banner="CURRENT MARKET LONG CONFIRMATION",
    learners=5,
    generations=8,
    population=10,
    train_games=50,
    eval_games=2500,
    output_directory="current-market-confirmation",
    archive_name="current-market-confirmation-results.tar.gz",
)

SANITY_PROFILE = MarketExperimentProfile(
    name="current-market-sanity",
    description="CURRENT MARKET SANITY CHECK — fresh Buy-only learners across 2p/3p/4p",
    banner="CURRENT MARKET SANITY CHECK",
    learners=3,
    generations=5,
    population=8,
    train_games=35,
    eval_games=500,
    output_directory="current-market-sanity",
    archive_name="current-market-sanity-results.tar.gz",
)


@dataclass(frozen=True)
class ConfirmationConfig:
    project_root: Path
    profile: MarketExperimentProfile = CONFIRMATION_PROFILE
    player_counts: tuple[int, ...] = DEFAULT_PLAYER_COUNTS
    learners: int = 5
    generations: int = 8
    population: int = 10
    train_games: int = 50
    eval_games: int = 2500
    elites: int = 2
    sigma: float = 0.25
    mutations: int = 6
    resume: bool = True
    rounds: str = "3/2/2"
    plant_overrides: Path | None = None
    round_overrides: Path | None = None
    buy_template: Path | None = None
    output_root: Path | None = None
    train_grove_seed_base: int = 3_810_000
    eval_grove_seed_base: int = 3_910_000
    eval_mechanical_seed_base: int = 4_010_000
    eval_strategy_seed_base: int = 4_110_000

    def __post_init__(self) -> None:
        root = self.project_root.resolve()
        object.__setattr__(self, "project_root", root)
        if self.plant_overrides is None:
            object.__setattr__(
                self,
                "plant_overrides",
                root / "data/research/4p/resync/resync-current.csv",
            )
        else:
            object.__setattr__(self, "plant_overrides", _resolve_under(root, self.plant_overrides))
        if self.round_overrides is None:
            object.__setattr__(
                self,
                "round_overrides",
                root / "data/research/4p/resync/round-resync-current.csv",
            )
        else:
            object.__setattr__(self, "round_overrides", _resolve_under(root, self.round_overrides))
        if self.buy_template is None:
            object.__setattr__(self, "buy_template", root / "data/ai/4p/buy-policy-v1.weights")
        else:
            object.__setattr__(self, "buy_template", _resolve_under(root, self.buy_template))
        if self.output_root is None:
            object.__setattr__(
                self,
                "output_root",
                root / "output/experiments" / self.profile.output_directory,
            )
        else:
            object.__setattr__(self, "output_root", _resolve_under(root, self.output_root))

        if not self.player_counts or any(players not in (2, 3, 4) for players in self.player_counts):
            raise ValueError("player_counts must contain only 2, 3, and/or 4")
        if len(set(self.player_counts)) != len(self.player_counts):
            raise ValueError("player_counts must not contain duplicates")
        if self.learners <= 0:
            raise ValueError("learners must be positive")
        if self.generations <= 0 or self.population < 2 or self.train_games <= 0 or self.eval_games <= 0:
            raise ValueError("training/evaluation scales must be positive")
        if self.elites < 1 or self.elites >= self.population:
            raise ValueError("elites must be between 1 and population-1")

    @classmethod
    def from_environment(
        cls,
        *,
        profile: MarketExperimentProfile = CONFIRMATION_PROFILE,
        project_root: Path | str | None = None,
        environ: Mapping[str, str] | None = None,
    ) -> "ConfirmationConfig":
        env = os.environ if environ is None else environ
        root = Path(project_root).resolve() if project_root is not None else find_project_root()

        def env_int(name: str, default: int) -> int:
            return int(env.get(name, str(default)))

        def env_float(name: str, default: float) -> float:
            return float(env.get(name, str(default)))

        player_counts = tuple(int(part) for part in env.get("PLAYERS_LIST", "2 3 4").split())
        return cls(
            project_root=root,
            profile=profile,
            player_counts=player_counts,
            learners=env_int("LEARNERS", profile.learners),
            generations=env_int("GENERATIONS", profile.generations),
            population=env_int("POPULATION", profile.population),
            train_games=env_int("TRAIN_GAMES", profile.train_games),
            eval_games=env_int("EVAL_GAMES", profile.eval_games),
            elites=env_int("ELITES", 2),
            sigma=env_float("SIGMA", 0.25),
            mutations=env_int("MUTATIONS", 6),
            resume=env.get("RESUME", "1") not in {"0", "false", "False", "no", "NO"},
            rounds=env.get("ROUNDS", "3/2/2"),
            plant_overrides=Path(env.get("PLANT_OVERRIDES", "data/research/4p/resync/resync-current.csv")),
            round_overrides=Path(env.get("ROUND_OVERRIDES", "data/research/4p/resync/round-resync-current.csv")),
            buy_template=Path(env.get("BUY_TEMPLATE", "data/ai/4p/buy-policy-v1.weights")),
            output_root=Path(env.get("OUTPUT_ROOT", f"output/experiments/{profile.output_directory}")),
            train_grove_seed_base=env_int("TRAIN_GROVE_SEED", 3_810_000),
            eval_grove_seed_base=env_int("EVAL_GROVE_SEED", 3_910_000),
            eval_mechanical_seed_base=env_int("EVAL_MECHANICAL_SEED", 4_010_000),
            eval_strategy_seed_base=env_int("EVAL_STRATEGY_SEED", 4_110_000),
        )


@dataclass(frozen=True)
class SeedPlan:
    evolution_seed: int
    train_mechanical_seed: int
    train_strategy_seed: int
    train_grove_seed: int
    eval_mechanical_seed: int
    eval_strategy_seed: int
    eval_grove_seed: int


@dataclass(frozen=True)
class LearnerPaths:
    weights: Path
    train_log: Path
    train_complete: Path
    eval_log: Path
    eval_complete: Path
    market_json: Path


@dataclass(frozen=True)
class ConfirmationOutcome:
    output_root: Path
    archive: Path
    raw_results: tuple[MarketRawResult, ...]


ProcessExecutor = Callable[[Sequence[str], Path, Path], None]
ResultLoader = Callable[[Path], MarketRawResult]
ReportWriter = Callable[[Iterable[MarketRawResult], Path], object]
Archiver = Callable[[Path, Path], Path]


def seed_plan(config: ConfirmationConfig, players: int, learner: int) -> SeedPlan:
    if players not in (2, 3, 4):
        raise ValueError(f"unsupported player count: {players}")
    if learner <= 0:
        raise ValueError("learner must be positive")
    return SeedPlan(
        evolution_seed=4_210_000 + players * 10_000 + learner * 101,
        train_mechanical_seed=4_310_000 + players * 100_000 + learner * 1_000,
        train_strategy_seed=4_410_000 + players * 100_000 + learner * 1_000,
        train_grove_seed=config.train_grove_seed_base + players * 100_000 + learner * 1_000,
        eval_mechanical_seed=config.eval_mechanical_seed_base + players * 100_000,
        eval_strategy_seed=config.eval_strategy_seed_base + players * 100_000,
        eval_grove_seed=config.eval_grove_seed_base + players * 100_000,
    )


def learner_paths(config: ConfirmationConfig, players: int, learner: int) -> LearnerPaths:
    base = config.output_root / f"{players}p"
    weights = base / "weights" / f"learner-{learner}.weights"
    train_log = base / "train" / f"learner-{learner}.log"
    eval_log = base / "eval" / f"learner-{learner}.log"
    return LearnerPaths(
        weights=weights,
        train_log=train_log,
        train_complete=Path(f"{train_log}.complete"),
        eval_log=eval_log,
        eval_complete=Path(f"{eval_log}.complete"),
        market_json=base / "eval" / f"learner-{learner}.market.json",
    )


def build_train_command(
    config: ConfirmationConfig,
    players: int,
    learner: int,
    zero_weights: Path,
) -> list[str]:
    paths = learner_paths(config, players, learner)
    seeds = seed_plan(config, players, learner)
    return [
        str(config.project_root / "bin/train_buy_policy"),
        "--players", str(players),
        "--input", str(zero_weights),
        "--output", str(paths.weights),
        "--plant-overrides", str(config.plant_overrides),
        "--round-overrides", str(config.round_overrides),
        "--random-grove",
        "--grove-seed", str(seeds.train_grove_seed),
        "--generations", str(config.generations),
        "--population", str(config.population),
        "--games", str(config.train_games),
        "--elites", str(config.elites),
        "--sigma", str(config.sigma),
        "--mutations", str(config.mutations),
        "--evolution-seed", str(seeds.evolution_seed),
        "--seed", str(seeds.train_mechanical_seed),
        "--strategy-seed", str(seeds.train_strategy_seed),
    ]


def build_eval_command(config: ConfirmationConfig, players: int, learner: int) -> list[str]:
    paths = learner_paths(config, players, learner)
    seeds = seed_plan(config, players, learner)
    return [
        str(config.project_root / "bin/evaluate_buy_policy"),
        "--players", str(players),
        "--weights", str(paths.weights),
        "--games", str(config.eval_games),
        "--seed", str(seeds.eval_mechanical_seed),
        "--strategy-seed", str(seeds.eval_strategy_seed),
        "--random-grove",
        "--grove-seed", str(seeds.eval_grove_seed),
        "--rounds", config.rounds,
        "--plant-overrides", str(config.plant_overrides),
        "--round-overrides", str(config.round_overrides),
        "--market-json", str(paths.market_json),
    ]


def create_zero_weight_file(template: Path, destination: Path) -> None:
    """Create a neutral Buy-policy seed while preserving the current card manifest.

    This intentionally mirrors the historical shell helper's semantics: format,
    policy, manifest version, card metadata, and comments are retained; recorded
    training provenance is removed; all remaining numeric feature weights are set
    to 0.0.
    """

    lines: list[str] = []
    for raw in template.read_text(encoding="utf-8").splitlines():
        if raw.startswith(("formatVersion=", "policy=", "cardManifestFormatVersion=", "card.", "#")):
            lines.append(raw)
        elif raw.startswith(("training", "trained")):
            continue
        elif "=" in raw:
            key = raw.split("=", 1)[0]
            lines.append(f"{key}=0.0")
        else:
            lines.append(raw)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text("\n".join(lines) + "\n", encoding="utf-8")


def default_process_executor(command: Sequence[str], log_path: Path, cwd: Path) -> None:
    """Run one Kotlin wrapper, streaming combined stdout/stderr to console and log."""

    log_path.parent.mkdir(parents=True, exist_ok=True)
    with log_path.open("w", encoding="utf-8") as log:
        process = subprocess.Popen(
            list(command),
            cwd=cwd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
        )
        assert process.stdout is not None
        for line in process.stdout:
            print(line, end="", flush=True)
            log.write(line)
            log.flush()
        return_code = process.wait()
    if return_code != 0:
        raise subprocess.CalledProcessError(return_code, list(command))


def train_is_complete(config: ConfirmationConfig, paths: LearnerPaths) -> bool:
    return config.resume and paths.train_complete.is_file() and paths.weights.is_file()


def eval_is_complete(config: ConfirmationConfig, paths: LearnerPaths) -> bool:
    return (
        config.resume
        and paths.eval_complete.is_file()
        and paths.eval_log.is_file()
        and paths.market_json.is_file()
    )


def validate_preflight(config: ConfirmationConfig) -> None:
    required_files = (config.plant_overrides, config.round_overrides, config.buy_template)
    for path in required_files:
        if not path.is_file():
            raise FileNotFoundError(f"missing required file: {path}")
    for relative in ("bin/train_buy_policy", "bin/evaluate_buy_policy"):
        path = config.project_root / relative
        if not path.is_file() or not os.access(path, os.X_OK):
            raise FileNotFoundError(f"missing executable: {path}")


def write_experiment_config(config: ConfirmationConfig) -> Path:
    config_dir = config.output_root / "config"
    config_dir.mkdir(parents=True, exist_ok=True)
    plant_copy = config_dir / "plant-resync-current.csv"
    round_copy = config_dir / "round-resync-current.csv"
    shutil.copy2(config.plant_overrides, plant_copy)
    shutil.copy2(config.round_overrides, round_copy)

    config_path = config_dir / "experiment.txt"
    values = {
        "experiment": config.profile.name,
        "description": config.profile.description,
        "players": " ".join(map(str, config.player_counts)),
        "learnersPerPlayerCount": str(config.learners),
        "generations": str(config.generations),
        "population": str(config.population),
        "trainingGamesPerPolicy": str(config.train_games),
        "heldOutMatchedSamples": str(config.eval_games),
        "rounds": config.rounds,
        "plantOverrides": _display_path(config.project_root, config.plant_overrides),
        "plantSha256": _sha256(config.plant_overrides),
        "roundOverrides": _display_path(config.project_root, config.round_overrides),
        "roundSha256": _sha256(config.round_overrides),
        "buyInitialization": "zero weights derived only for current card manifest",
        "allOtherDecisionFamilies": "Human Baseline",
        "grove": "random legal 4x9, independently resolved per sample",
        "rawMarketTelemetry": "typed Kotlin JSON; no console parsing",
        "reporting": "tested Python typed loader/aggregation/report writers",
    }
    config_path.write_text("".join(f"{key}={value}\n" for key, value in values.items()), encoding="utf-8")
    return config_path


def write_manifest(config: ConfirmationConfig, raw_results: Sequence[MarketRawResult]) -> Path:
    manifest = config.output_root / "config" / "manifest.txt"
    lines = [
        "schema=current-market-experiment-manifest-v1",
        f"experiment={config.profile.name}",
        f"rawResultCount={len(raw_results)}",
    ]
    for result in sorted(raw_results, key=lambda item: (item.metadata.players, item.metadata.policy_path)):
        source = result.source_path
        lines.append(
            "rawResult="
            + (_display_path(config.project_root, source) if source is not None else result.metadata.policy_path)
        )
    manifest.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return manifest


def create_archive(output_root: Path, archive_path: Path) -> Path:
    """Create a deterministic-ish gzip tar without recursively archiving itself."""

    archive_path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(
        prefix=f".{archive_path.name}.", suffix=".tmp", dir=archive_path.parent, delete=False
    ) as tmp:
        temp_path = Path(tmp.name)
    try:
        with tarfile.open(temp_path, "w:gz") as tar:
            for path in sorted(output_root.rglob("*")):
                if path == archive_path or path == temp_path:
                    continue
                arcname = Path(output_root.name) / path.relative_to(output_root)
                tar.add(path, arcname=str(arcname), recursive=False)
        temp_path.replace(archive_path)
    except Exception:
        temp_path.unlink(missing_ok=True)
        raise
    return archive_path


class ProgressTracker:
    def __init__(self, total_steps: int) -> None:
        self.total_steps = total_steps
        self.completed_steps = 0
        self.started = time.monotonic()

    def start(self, label: str) -> None:
        print()
        print("================================================================")
        print(label)
        print(f"Clock: {datetime.now().strftime('%Y-%m-%d %I:%M:%S %p')}")
        print("================================================================")

    def complete(self) -> None:
        self.completed_steps += 1
        elapsed = max(0.0, time.monotonic() - self.started)
        average = elapsed / self.completed_steps if self.completed_steps else 0.0
        remaining = average * max(0, self.total_steps - self.completed_steps)
        eta = datetime.now() + timedelta(seconds=remaining)
        print(
            f"OVERALL_PROGRESS runs={self.completed_steps}/{self.total_steps} "
            f"elapsed={_duration(elapsed)} estRemaining={_duration(remaining)} "
            f"ETA={eta.strftime('%a %I:%M %p')}"
        )


def run_market_experiment(
    config: ConfirmationConfig,
    *,
    executor: ProcessExecutor = default_process_executor,
    loader: ResultLoader = load_market_result,
    report_writer: ReportWriter = write_market_reports,
    archiver: Archiver = create_archive,
) -> ConfirmationOutcome:
    validate_preflight(config)
    config.output_root.mkdir(parents=True, exist_ok=True)
    write_experiment_config(config)

    zero_weights = config.output_root / "config" / "buy-zero-start.weights"
    create_zero_weight_file(config.buy_template, zero_weights)

    progress = ProgressTracker(len(config.player_counts) * config.learners * 2)
    raw_results: list[MarketRawResult] = []

    print("================================================================")
    print(f"{config.profile.banner} — PYTHON ORCHESTRATOR")
    print("================================================================")
    print(f"Player counts:          {' '.join(map(str, config.player_counts))}")
    print(f"Learners/count:         {config.learners}")
    print(
        "Training:               "
        f"generations={config.generations} population={config.population} games/policy={config.train_games}"
    )
    print(f"Held-out samples:       {config.eval_games} per learner")
    print("Buy initialization:     ZERO weights")
    print("Other decision hooks:   Human Baseline")
    print("Grove:                  random legal market")
    print("Reporting:              typed Kotlin JSON -> validated/tested Python")
    print("================================================================")

    for players in config.player_counts:
        for learner in range(1, config.learners + 1):
            paths = learner_paths(config, players, learner)
            for directory in (paths.weights.parent, paths.train_log.parent, paths.eval_log.parent):
                directory.mkdir(parents=True, exist_ok=True)

            progress.start(f"Train {players}p learner {learner}/{config.learners}")
            if train_is_complete(config, paths):
                print("Already complete; resume marker and weights are present.")
            else:
                paths.train_complete.unlink(missing_ok=True)
                executor(build_train_command(config, players, learner, zero_weights), paths.train_log, config.project_root)
                if not paths.weights.is_file():
                    raise RuntimeError(f"training completed without expected weights: {paths.weights}")
                paths.train_complete.touch()
            progress.complete()

            progress.start(f"Evaluate {players}p learner {learner}/{config.learners}")
            if eval_is_complete(config, paths):
                print("Already complete; resume marker, log, and structured JSON are present.")
            else:
                paths.eval_complete.unlink(missing_ok=True)
                paths.market_json.unlink(missing_ok=True)
                executor(build_eval_command(config, players, learner), paths.eval_log, config.project_root)
                if not paths.market_json.is_file():
                    raise RuntimeError(f"evaluation completed without expected market JSON: {paths.market_json}")
                # Validation is part of successful completion. Never mark a malformed result complete.
                loader(paths.market_json)
                paths.eval_complete.touch()
            progress.complete()

            # Always load again after resume or fresh completion so downstream reporting
            # consumes only validated structured results from disk.
            raw_results.append(loader(paths.market_json))

    reports_dir = config.output_root / "reports"
    report_writer(tuple(raw_results), reports_dir)
    write_manifest(config, raw_results)

    archive = config.output_root / config.profile.archive_name
    archiver(config.output_root, archive)

    print()
    print("DONE")
    print(f"Read first:            {reports_dir / 'README-FIRST.txt'}")
    print(f"Card summary:          {reports_dir / 'card-summary.tsv'}")
    print(f"Slot summary:          {reports_dir / 'slot-summary.tsv'}")
    print(f"Zero-purchase cards:   {reports_dir / 'zero-purchase-cards.tsv'}")
    print(f"Vine-9 focus:          {reports_dir / 'vine-9-summary.tsv'}")
    print(f"Learner win shares:    {reports_dir / 'learner-win-shares.tsv'}")
    print(f"Archive:               {archive}")

    return ConfirmationOutcome(config.output_root, archive, tuple(raw_results))


def run_confirmation(
    config: ConfirmationConfig,
    *,
    executor: ProcessExecutor = default_process_executor,
    loader: ResultLoader = load_market_result,
    report_writer: ReportWriter = write_market_reports,
    archiver: Archiver = create_archive,
) -> ConfirmationOutcome:
    """Backward-compatible name for the confirmation-profile runner."""

    return run_market_experiment(
        config,
        executor=executor,
        loader=loader,
        report_writer=report_writer,
        archiver=archiver,
    )


def main_for_profile(
    profile: MarketExperimentProfile, argv: Sequence[str] | None = None
) -> int:
    parser = argparse.ArgumentParser(description=f"Run the {profile.name} experiment.")
    parser.add_argument("--players", nargs="+", type=int, choices=(2, 3, 4))
    parser.add_argument("--learners", type=int)
    parser.add_argument("--generations", type=int)
    parser.add_argument("--population", type=int)
    parser.add_argument("--train-games", type=int)
    parser.add_argument("--eval-games", type=int)
    parser.add_argument("--resume", choices=("0", "1"))
    parser.add_argument("--output-root", type=Path)
    args = parser.parse_args(argv)

    base = ConfirmationConfig.from_environment(profile=profile)
    overrides = {
        "project_root": base.project_root,
        "profile": profile,
        "player_counts": tuple(args.players) if args.players else base.player_counts,
        "learners": args.learners if args.learners is not None else base.learners,
        "generations": args.generations if args.generations is not None else base.generations,
        "population": args.population if args.population is not None else base.population,
        "train_games": args.train_games if args.train_games is not None else base.train_games,
        "eval_games": args.eval_games if args.eval_games is not None else base.eval_games,
        "elites": base.elites,
        "sigma": base.sigma,
        "mutations": base.mutations,
        "resume": (args.resume == "1") if args.resume is not None else base.resume,
        "rounds": base.rounds,
        "plant_overrides": base.plant_overrides,
        "round_overrides": base.round_overrides,
        "buy_template": base.buy_template,
        "output_root": args.output_root if args.output_root is not None else base.output_root,
        "train_grove_seed_base": base.train_grove_seed_base,
        "eval_grove_seed_base": base.eval_grove_seed_base,
        "eval_mechanical_seed_base": base.eval_mechanical_seed_base,
        "eval_strategy_seed_base": base.eval_strategy_seed_base,
    }
    run_market_experiment(ConfirmationConfig(**overrides))
    return 0


def main(argv: Sequence[str] | None = None) -> int:
    return main_for_profile(CONFIRMATION_PROFILE, argv)


def _resolve_under(root: Path, path: Path) -> Path:
    return path.resolve() if path.is_absolute() else (root / path).resolve()


def _display_path(root: Path, path: Path | None) -> str:
    if path is None:
        return ""
    try:
        return str(path.relative_to(root))
    except ValueError:
        return str(path)


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _duration(seconds: float) -> str:
    total = int(max(0.0, seconds))
    hours, remainder = divmod(total, 3600)
    minutes, secs = divmod(remainder, 60)
    return f"{hours:02d}:{minutes:02d}:{secs:02d}"


if __name__ == "__main__":
    raise SystemExit(main())
