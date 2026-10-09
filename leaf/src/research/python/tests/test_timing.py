from __future__ import annotations

from datetime import datetime
import io
from pathlib import Path
import sys
import time

import pytest

from leaf_experiments.timing import (
    ExperimentTimer,
    RepeatedConsoleSetupFilter,
    format_duration,
    heartbeat_seconds,
    run_streaming_process,
)


def test_format_duration() -> None:
    assert format_duration(0) == "00:00:00"
    assert format_duration(65) == "00:01:05"
    assert format_duration(3661.9) == "01:01:01"
    assert format_duration(-5) == "00:00:00"


def test_heartbeat_interval_default_and_override() -> None:
    assert heartbeat_seconds({}) == 60.0
    assert heartbeat_seconds({"LEAF_PROGRESS_HEARTBEAT_SECONDS": "12.5"}) == 12.5
    with pytest.raises(ValueError):
        heartbeat_seconds({"LEAF_PROGRESS_HEARTBEAT_SECONDS": "0"})


def test_experiment_timer_excludes_skips_and_reports_eta() -> None:
    mono_values = iter([0.0, 0.0, 10.0, 10.0, 10.0, 20.0])
    clock = datetime(2026, 10, 8, 12, 0, 0)
    output: list[str] = []

    def mono() -> float:
        return next(mono_values)

    def printer(message="", **kwargs) -> None:
        output.append(str(message))

    timer = ExperimentTimer(
        ("train", "eval"),
        now_monotonic=mono,
        now_datetime=lambda: clock,
        printer=printer,
    )
    timer.start("Train learner", "train")
    timer.complete(skipped=False)
    timer.start("Evaluate learner", "eval")
    timer.complete(skipped=True)

    joined = "\n".join(output)
    assert "Run 1 of 2 starting: Train learner" in joined
    assert "RUN_COMPLETE 1/2 took 00:00:10" in joined
    assert "OVERALL_PROGRESS runs=1/2" in joined
    assert "estRemaining=00:00:10" in joined
    assert "RUN_COMPLETE 2/2 SKIPPED" in joined
    assert "remainingWork=0" in joined


def test_streaming_process_emits_heartbeat_when_child_is_silent(tmp_path: Path) -> None:
    log = io.StringIO()
    output: list[str] = []

    def printer(message="", **kwargs) -> None:
        end = kwargs.get("end", "\n")
        output.append(str(message) + ("" if end == "" else end))

    run_streaming_process(
        [sys.executable, "-c", "import time; time.sleep(0.12); print('done')"],
        cwd=tmp_path,
        log=log,
        heartbeat_interval=0.03,
        printer=printer,
    )

    joined = "".join(output)
    assert "PROCESS_PROGRESS" in joined
    assert "PROCESS_COMPLETE" in joined
    assert "done" in joined
    assert "done" in log.getvalue()


def test_repeated_console_setup_filter_shows_first_identical_block_only() -> None:
    f = RepeatedConsoleSetupFilter()
    block = [
        "PLANT EXPERIMENT OVERRIDES\n",
        "source: /tmp/current.csv\n",
        "Root_07_01\n",
        "  cost: 7 -> 9\n",
        "All unspecified Plant properties canonical.\n",
    ]
    first = [visible for line in block for visible in f.feed(line)]
    second = [visible for line in block for visible in f.feed(line)]
    assert first == block
    assert second == []


def test_repeated_console_setup_filter_shows_changed_block() -> None:
    f = RepeatedConsoleSetupFilter()
    first_block = [
        "ROUND EXPERIMENT OVERRIDES\n",
        "source: /tmp/a.csv\n",
        "All unspecified Round effects canonical.\n",
    ]
    changed_block = [
        "ROUND EXPERIMENT OVERRIDES\n",
        "source: /tmp/b.csv\n",
        "All unspecified Round effects canonical.\n",
    ]
    assert [v for line in first_block for v in f.feed(line)] == first_block
    assert [v for line in changed_block for v in f.feed(line)] == changed_block


def test_repeated_console_setup_filter_deduplicates_sample_zero_environment_lines() -> None:
    f = RepeatedConsoleSetupFilter()
    line = "resolved research environment sample 0 Round deck=<normal game setup>\n"
    assert f.feed(line) == [line]
    assert f.feed(line) == []


def test_streaming_process_keeps_suppressed_setup_in_log(tmp_path: Path) -> None:
    log = io.StringIO()
    output: list[str] = []
    filter_ = RepeatedConsoleSetupFilter()

    def printer(message="", **kwargs) -> None:
        end = kwargs.get("end", "\n")
        output.append(str(message) + ("" if end == "" else end))

    code = (
        "print('PLANT EXPERIMENT OVERRIDES'); "
        "print('source: current.csv'); "
        "print('All unspecified Plant properties canonical.')"
    )
    run_streaming_process(
        [sys.executable, "-c", code], cwd=tmp_path, log=log, heartbeat_interval=1, printer=printer,
        console_filter=filter_,
    )
    first_console = "".join(output)
    output.clear()
    run_streaming_process(
        [sys.executable, "-c", code], cwd=tmp_path, log=log, heartbeat_interval=1, printer=printer,
        console_filter=filter_,
    )
    second_console = "".join(output)
    assert "PLANT EXPERIMENT OVERRIDES" in first_console
    assert "PLANT EXPERIMENT OVERRIDES" not in second_console
    assert log.getvalue().count("PLANT EXPERIMENT OVERRIDES") == 2


def test_overall_eta_ignores_future_resume_skips_and_prefers_matching_category() -> None:
    current = [0.0]
    clock = datetime(2026, 10, 8, 13, 0, 0)
    output: list[str] = []

    def mono() -> float:
        return current[0]

    def printer(message="", **kwargs) -> None:
        output.append(str(message))

    timer = ExperimentTimer(
        ("train-4p", "eval-4p", "train-4p", "eval-4p"),
        planned_skips=(False, False, True, False),
        now_monotonic=mono,
        now_datetime=lambda: clock,
        printer=printer,
    )

    timer.start("Train 4p learner 1/2", "train-4p")
    current[0] = 10.0
    timer.complete()

    timer.start("Evaluate 4p learner 1/2", "eval-4p")
    current[0] = 610.0
    timer.complete()

    joined = "\n".join(output)
    # Only one real operation remains: the second 4p evaluation.  Its ETA must
    # use the measured 4p-evaluation duration (10 minutes), not average in the
    # skipped train step or the 10-second training duration.
    assert "OVERALL_PROGRESS runs=2/4" in joined
    assert "remainingWork=1" in joined
    assert "estRemaining=00:10:00" in joined
    assert "estimatedExperimentFinish=Thu Oct 08 01:10 PM" in joined


def test_planned_skips_length_must_match_categories() -> None:
    with pytest.raises(ValueError, match="planned_skips"):
        ExperimentTimer(("train", "eval"), planned_skips=(True,))


def test_experiment_plan_runner_makes_timing_mandatory() -> None:
    from leaf_experiments.timing import ExperimentPlanRunner, ExperimentStep

    current = [0.0]
    output: list[str] = []
    ran: list[str] = []

    def mono() -> float:
        return current[0]

    def printer(message="", **kwargs) -> None:
        output.append(str(message))

    def first() -> None:
        ran.append("first")
        current[0] = 5.0

    def second() -> None:
        ran.append("second")
        current[0] = 12.0

    runner = ExperimentPlanRunner(
        (
            ExperimentStep("Train", "train-2p", first),
            ExperimentStep("Eval", "eval-2p", second),
        ),
        printer=printer,
        now_monotonic=mono,
        now_datetime=lambda: datetime(2026, 10, 9, 12, 0, 0),
    )
    runner.run(description="Done")

    assert ran == ["first", "second"]
    joined = "\n".join(output)
    assert "Run 1 of 2 starting: Train" in joined
    assert "OVERALL_PROGRESS runs=1/2" in joined
    assert "OVERALL_PROGRESS runs=2/2" in joined
    assert "Done; elapsed=00:00:12" in joined
