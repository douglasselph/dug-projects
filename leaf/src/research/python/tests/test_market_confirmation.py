from __future__ import annotations

from dataclasses import replace
from pathlib import Path
import subprocess
import tarfile

import pytest

from leaf_experiments.market.confirmation import (
    ConfirmationConfig,
    build_eval_command,
    build_train_command,
    create_archive,
    create_zero_weight_file,
    eval_is_complete,
    learner_paths,
    run_confirmation,
    seed_plan,
    train_is_complete,
)


def make_project(tmp_path: Path) -> Path:
    root = tmp_path / "leaf"
    (root / "bin").mkdir(parents=True)
    (root / "data/research/4p/resync").mkdir(parents=True)
    (root / "data/ai/4p").mkdir(parents=True)
    (root / "output").mkdir(parents=True)
    (root / "settings.gradle.kts").write_text("", encoding="utf-8")
    (root / "build.gradle.kts").write_text("", encoding="utf-8")

    for name in ("train_buy_policy", "evaluate_buy_policy"):
        path = root / "bin" / name
        path.write_text("#!/usr/bin/env bash\nexit 0\n", encoding="utf-8")
        path.chmod(0o755)

    (root / "data/research/4p/resync/resync-current.csv").write_text("plant\n", encoding="utf-8")
    (root / "data/research/4p/resync/round-resync-current.csv").write_text("round\n", encoding="utf-8")
    (root / "data/ai/4p/buy-policy-v1.weights").write_text(
        "\n".join(
            [
                "formatVersion=3",
                "policy=buy-v1",
                "trainingStatus=trained",
                "trainingGamesPerPolicy=50",
                "cardManifestFormatVersion=1",
                "card.Root_05_01.title=Root Double Down",
                "card.Root_05_01.fingerprint=abc",
                "BIAS=1.5",
                "SELF_VP=-3.0",
            ]
        )
        + "\n",
        encoding="utf-8",
    )
    return root


def make_config(tmp_path: Path, **changes) -> ConfirmationConfig:
    root = make_project(tmp_path)
    config = ConfirmationConfig(
        project_root=root,
        player_counts=(2,),
        learners=1,
        generations=8,
        population=10,
        train_games=50,
        eval_games=2500,
    )
    return replace(config, **changes) if changes else config


def test_seed_plan_matches_historical_confirmation_formulas(tmp_path: Path) -> None:
    config = make_config(tmp_path)
    seeds = seed_plan(config, 2, 3)
    assert seeds.evolution_seed == 4_230_303
    assert seeds.train_mechanical_seed == 4_513_000
    assert seeds.train_strategy_seed == 4_613_000
    assert seeds.train_grove_seed == 4_013_000
    assert seeds.eval_mechanical_seed == 4_210_000
    assert seeds.eval_strategy_seed == 4_310_000
    assert seeds.eval_grove_seed == 4_110_000


def test_train_command_preserves_confirmation_design(tmp_path: Path) -> None:
    config = make_config(tmp_path)
    zero = config.output_root / "config/buy-zero-start.weights"
    command = build_train_command(config, 2, 1, zero)
    assert command[0] == str(config.project_root / "bin/train_buy_policy")
    assert _arg(command, "--players") == "2"
    assert _arg(command, "--input") == str(zero)
    assert _arg(command, "--output").endswith("2p/weights/learner-1.weights")
    assert "--random-grove" in command
    assert _arg(command, "--generations") == "8"
    assert _arg(command, "--population") == "10"
    assert _arg(command, "--games") == "50"
    assert _arg(command, "--grove-seed") == "4011000"
    assert _arg(command, "--evolution-seed") == "4230101"
    assert _arg(command, "--seed") == "4511000"
    assert _arg(command, "--strategy-seed") == "4611000"


def test_eval_command_uses_matched_schedule_and_structured_json(tmp_path: Path) -> None:
    config = make_config(tmp_path)
    command = build_eval_command(config, 2, 4)
    paths = learner_paths(config, 2, 4)
    assert command[0] == str(config.project_root / "bin/evaluate_buy_policy")
    assert _arg(command, "--weights") == str(paths.weights)
    assert _arg(command, "--games") == "2500"
    assert _arg(command, "--seed") == "4210000"
    assert _arg(command, "--strategy-seed") == "4310000"
    assert _arg(command, "--grove-seed") == "4110000"
    assert _arg(command, "--rounds") == "3/2/2"
    assert _arg(command, "--market-json") == str(paths.market_json)
    assert "--random-grove" in command


def test_zero_weight_file_preserves_manifest_and_removes_training_provenance(tmp_path: Path) -> None:
    config = make_config(tmp_path)
    destination = config.output_root / "config/zero.weights"
    create_zero_weight_file(config.buy_template, destination)
    text = destination.read_text(encoding="utf-8")
    assert "formatVersion=3" in text
    assert "policy=buy-v1" in text
    assert "cardManifestFormatVersion=1" in text
    assert "card.Root_05_01.title=Root Double Down" in text
    assert "card.Root_05_01.fingerprint=abc" in text
    assert "trainingStatus" not in text
    assert "trainingGamesPerPolicy" not in text
    assert "BIAS=0.0" in text
    assert "SELF_VP=0.0" in text


def test_resume_requires_expected_artifacts(tmp_path: Path) -> None:
    config = make_config(tmp_path)
    paths = learner_paths(config, 2, 1)
    paths.weights.parent.mkdir(parents=True, exist_ok=True)
    paths.train_log.parent.mkdir(parents=True, exist_ok=True)
    paths.train_complete.touch()
    assert not train_is_complete(config, paths)
    paths.weights.touch()
    assert train_is_complete(config, paths)

    paths.eval_log.parent.mkdir(parents=True, exist_ok=True)
    paths.eval_complete.touch()
    paths.eval_log.touch()
    assert not eval_is_complete(config, paths)
    paths.market_json.touch()
    assert eval_is_complete(config, paths)


def test_resume_false_never_skips(tmp_path: Path) -> None:
    config = make_config(tmp_path, resume=False)
    paths = learner_paths(config, 2, 1)
    for path in (
        paths.weights,
        paths.train_complete,
        paths.eval_log,
        paths.eval_complete,
        paths.market_json,
    ):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.touch()
    assert not train_is_complete(config, paths)
    assert not eval_is_complete(config, paths)


def test_run_confirmation_invokes_training_eval_loader_reports_and_archive(tmp_path: Path) -> None:
    config = make_config(tmp_path)
    calls: list[tuple[str, ...]] = []
    loaded: list[Path] = []
    report_calls: list[tuple[tuple[object, ...], Path]] = []
    archive_calls: list[tuple[Path, Path]] = []
    sentinel = object()

    def executor(command, log_path, cwd):
        calls.append(tuple(command))
        log_path.parent.mkdir(parents=True, exist_ok=True)
        log_path.write_text("fake process log\n", encoding="utf-8")
        if command[0].endswith("train_buy_policy"):
            output = Path(_arg(command, "--output"))
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_text("trained\n", encoding="utf-8")
        else:
            raw = Path(_arg(command, "--market-json"))
            raw.parent.mkdir(parents=True, exist_ok=True)
            raw.write_text("{}\n", encoding="utf-8")

    def loader(path):
        loaded.append(path)
        return sentinel

    def reports(results, output_dir):
        materialized = tuple(results)
        report_calls.append((materialized, output_dir))
        output_dir.mkdir(parents=True, exist_ok=True)
        (output_dir / "README-FIRST.txt").write_text("ok\n", encoding="utf-8")
        return {}

    def archiver(output_root, archive_path):
        archive_calls.append((output_root, archive_path))
        archive_path.write_bytes(b"archive")
        return archive_path

    # Avoid manifest requiring real MarketRawResult fields by substituting a tiny
    # object with just the attributes it consumes.
    class Meta:
        players = 2
        policy_path = "learner-1.weights"

    class Raw:
        metadata = Meta()
        source_path = config.output_root / "2p/eval/learner-1.market.json"

    sentinel = Raw()

    outcome = run_confirmation(
        config,
        executor=executor,
        loader=loader,
        report_writer=reports,
        archiver=archiver,
    )
    assert len(calls) == 2
    assert calls[0][0].endswith("train_buy_policy")
    assert calls[1][0].endswith("evaluate_buy_policy")
    # Fresh eval is loaded once to validate before completion and again to feed reports.
    assert loaded == [
        config.output_root / "2p/eval/learner-1.market.json",
        config.output_root / "2p/eval/learner-1.market.json",
    ]
    assert report_calls == [((sentinel,), config.output_root / "reports")]
    assert archive_calls == [
        (config.output_root, config.output_root / "current-market-confirmation-results.tar.gz")
    ]
    assert outcome.raw_results == (sentinel,)


def test_resume_skips_processes_but_still_loads_json_and_regenerates_reports(tmp_path: Path) -> None:
    config = make_config(tmp_path)
    paths = learner_paths(config, 2, 1)
    for path in (paths.weights, paths.train_complete, paths.eval_log, paths.eval_complete, paths.market_json):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text("ready\n", encoding="utf-8")

    process_calls: list[object] = []
    loaded: list[Path] = []

    class Meta:
        players = 2
        policy_path = "learner-1.weights"

    class Raw:
        metadata = Meta()
        source_path = paths.market_json

    raw = Raw()

    def executor(*args):
        process_calls.append(args)

    def loader(path):
        loaded.append(path)
        return raw

    report_calls = []

    def reports(results, output_dir):
        report_calls.append((tuple(results), output_dir))
        output_dir.mkdir(parents=True, exist_ok=True)
        return {}

    def archiver(output_root, archive_path):
        archive_path.write_bytes(b"archive")
        return archive_path

    run_confirmation(config, executor=executor, loader=loader, report_writer=reports, archiver=archiver)
    assert process_calls == []
    assert loaded == [paths.market_json]
    assert report_calls == [((raw,), config.output_root / "reports")]


def test_subprocess_failure_propagates_and_does_not_create_completion_marker(tmp_path: Path) -> None:
    config = make_config(tmp_path)
    paths = learner_paths(config, 2, 1)

    def executor(command, log_path, cwd):
        log_path.parent.mkdir(parents=True, exist_ok=True)
        log_path.write_text("failed\n", encoding="utf-8")
        raise subprocess.CalledProcessError(7, command)

    with pytest.raises(subprocess.CalledProcessError):
        run_confirmation(config, executor=executor)
    assert not paths.train_complete.exists()
    assert not paths.eval_complete.exists()


def test_partial_run_keeps_completed_training_but_not_failed_evaluation(tmp_path: Path) -> None:
    config = make_config(tmp_path)
    paths = learner_paths(config, 2, 1)
    calls = 0

    def executor(command, log_path, cwd):
        nonlocal calls
        calls += 1
        log_path.parent.mkdir(parents=True, exist_ok=True)
        log_path.write_text("log\n", encoding="utf-8")
        if calls == 1:
            paths.weights.parent.mkdir(parents=True, exist_ok=True)
            paths.weights.write_text("trained\n", encoding="utf-8")
        else:
            raise subprocess.CalledProcessError(3, command)

    with pytest.raises(subprocess.CalledProcessError):
        run_confirmation(config, executor=executor)
    assert paths.train_complete.is_file()
    assert paths.weights.is_file()
    assert not paths.eval_complete.exists()
    assert not paths.market_json.exists()


def test_invalid_structured_result_does_not_get_completion_marker(tmp_path: Path) -> None:
    config = make_config(tmp_path)
    paths = learner_paths(config, 2, 1)

    def executor(command, log_path, cwd):
        log_path.parent.mkdir(parents=True, exist_ok=True)
        log_path.write_text("log\n", encoding="utf-8")
        if command[0].endswith("train_buy_policy"):
            paths.weights.parent.mkdir(parents=True, exist_ok=True)
            paths.weights.write_text("trained\n", encoding="utf-8")
        else:
            paths.market_json.parent.mkdir(parents=True, exist_ok=True)
            paths.market_json.write_text("bad json\n", encoding="utf-8")

    def bad_loader(path):
        raise ValueError("invalid structured result")

    with pytest.raises(ValueError, match="invalid structured result"):
        run_confirmation(config, executor=executor, loader=bad_loader)
    assert paths.train_complete.is_file()
    assert not paths.eval_complete.exists()


def test_archive_contains_outputs_but_not_itself(tmp_path: Path) -> None:
    output = tmp_path / "current-market-confirmation"
    (output / "reports").mkdir(parents=True)
    (output / "config").mkdir(parents=True)
    (output / "reports/card-summary.tsv").write_text("header\n", encoding="utf-8")
    (output / "config/experiment.txt").write_text("experiment=x\n", encoding="utf-8")
    archive = output / "current-market-confirmation-results.tar.gz"

    create_archive(output, archive)

    assert archive.is_file()
    with tarfile.open(archive, "r:gz") as tar:
        names = set(tar.getnames())
    assert "current-market-confirmation/reports/card-summary.tsv" in names
    assert "current-market-confirmation/config/experiment.txt" in names
    assert not any(name.endswith("current-market-confirmation-results.tar.gz") for name in names)


def test_environment_defaults_and_overrides_resolve_under_project_root(tmp_path: Path) -> None:
    root = make_project(tmp_path)
    config = ConfirmationConfig.from_environment(
        project_root=root,
        environ={
            "PLAYERS_LIST": "2 4",
            "LEARNERS": "3",
            "EVAL_GAMES": "777",
            "RESUME": "0",
            "OUTPUT_ROOT": "output/custom-market",
        },
    )
    assert config.player_counts == (2, 4)
    assert config.learners == 3
    assert config.eval_games == 777
    assert config.resume is False
    assert config.output_root == (root / "output/custom-market").resolve()


def _arg(command, name: str) -> str:
    index = list(command).index(name)
    return command[index + 1]


def test_confirmation_and_sanity_profiles_have_distinct_expected_scales(tmp_path: Path) -> None:
    from leaf_experiments.market.confirmation import CONFIRMATION_PROFILE, SANITY_PROFILE

    root = make_project(tmp_path)
    confirmation = ConfirmationConfig.from_environment(
        profile=CONFIRMATION_PROFILE, project_root=root, environ={}
    )
    sanity = ConfirmationConfig.from_environment(
        profile=SANITY_PROFILE, project_root=root, environ={}
    )

    assert confirmation.learners == 5
    assert confirmation.generations == 8
    assert confirmation.population == 10
    assert confirmation.train_games == 50
    assert confirmation.eval_games == 2500
    assert confirmation.output_root == (root / "output/experiments/current-market-confirmation").resolve()

    assert sanity.learners == 3
    assert sanity.generations == 5
    assert sanity.population == 8
    assert sanity.train_games == 35
    assert sanity.eval_games == 500
    assert sanity.output_root == (root / "output/experiments/current-market-sanity").resolve()


def test_profiles_share_same_command_builders_and_runner(tmp_path: Path) -> None:
    from leaf_experiments.market.confirmation import (
        CONFIRMATION_PROFILE,
        SANITY_PROFILE,
        run_market_experiment,
    )

    root = make_project(tmp_path)
    confirmation = ConfirmationConfig.from_environment(
        profile=CONFIRMATION_PROFILE,
        project_root=root,
        environ={"PLAYERS_LIST": "2", "LEARNERS": "1"},
    )
    sanity = ConfirmationConfig.from_environment(
        profile=SANITY_PROFILE,
        project_root=root,
        environ={"PLAYERS_LIST": "2", "LEARNERS": "1"},
    )

    confirmation_train = build_train_command(
        confirmation, 2, 1, confirmation.output_root / "config/buy-zero-start.weights"
    )
    sanity_train = build_train_command(
        sanity, 2, 1, sanity.output_root / "config/buy-zero-start.weights"
    )
    confirmation_eval = build_eval_command(confirmation, 2, 1)
    sanity_eval = build_eval_command(sanity, 2, 1)

    assert _arg(confirmation_train, "--generations") == "8"
    assert _arg(sanity_train, "--generations") == "5"
    assert _arg(confirmation_train, "--population") == "10"
    assert _arg(sanity_train, "--population") == "8"
    assert _arg(confirmation_train, "--games") == "50"
    assert _arg(sanity_train, "--games") == "35"
    assert _arg(confirmation_eval, "--games") == "2500"
    assert _arg(sanity_eval, "--games") == "500"
    assert run_market_experiment.__module__ == "leaf_experiments.market.confirmation"


def test_sanity_profile_writes_sanity_named_config_manifest_and_archive(tmp_path: Path) -> None:
    from leaf_experiments.market.confirmation import SANITY_PROFILE, run_market_experiment

    root = make_project(tmp_path)
    config = ConfirmationConfig.from_environment(
        profile=SANITY_PROFILE,
        project_root=root,
        environ={"PLAYERS_LIST": "2", "LEARNERS": "1"},
    )

    class Meta:
        players = 2
        policy_path = "learner-1.weights"

    class Raw:
        metadata = Meta()
        source_path = config.output_root / "2p/eval/learner-1.market.json"

    raw = Raw()

    def executor(command, log_path, cwd):
        log_path.parent.mkdir(parents=True, exist_ok=True)
        log_path.write_text("log\n", encoding="utf-8")
        if command[0].endswith("train_buy_policy"):
            output = Path(_arg(command, "--output"))
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_text("trained\n", encoding="utf-8")
        else:
            result = Path(_arg(command, "--market-json"))
            result.parent.mkdir(parents=True, exist_ok=True)
            result.write_text("{}\n", encoding="utf-8")

    def loader(path):
        return raw

    def reports(results, output_dir):
        output_dir.mkdir(parents=True, exist_ok=True)
        (output_dir / "README-FIRST.txt").write_text("ok\n", encoding="utf-8")
        return {}

    def archiver(output_root, archive_path):
        archive_path.write_bytes(b"archive")
        return archive_path

    outcome = run_market_experiment(
        config,
        executor=executor,
        loader=loader,
        report_writer=reports,
        archiver=archiver,
    )

    experiment_text = (config.output_root / "config/experiment.txt").read_text(encoding="utf-8")
    manifest_text = (config.output_root / "config/manifest.txt").read_text(encoding="utf-8")
    assert "experiment=current-market-sanity" in experiment_text
    assert "learnersPerPlayerCount=1" in experiment_text
    assert "generations=5" in experiment_text
    assert "population=8" in experiment_text
    assert "trainingGamesPerPolicy=35" in experiment_text
    assert "heldOutMatchedSamples=500" in experiment_text
    assert "experiment=current-market-sanity" in manifest_text
    assert outcome.archive.name == "current-market-sanity-results.tar.gz"
