"""Shared wall-clock timing/progress utilities for long-running research jobs.

The timing layer is deliberately operational only: it never affects seeds,
simulation state, or research results.  It exists so long-running experiments
always provide immediate progress, periodic heartbeats during silent child
processes, measured run durations, estimated remaining time, and an ETA.
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta
from queue import Empty, Queue
import os
from pathlib import Path
import subprocess
from threading import Thread
import time
from typing import Callable, Sequence, TextIO


DEFAULT_HEARTBEAT_SECONDS = 60.0


def format_duration(seconds: float) -> str:
    """Format a non-negative duration as HH:MM:SS."""

    total = int(max(0.0, seconds))
    hours, remainder = divmod(total, 3600)
    minutes, secs = divmod(remainder, 60)
    return f"{hours:02d}:{minutes:02d}:{secs:02d}"


def heartbeat_seconds(environ: dict[str, str] | None = None) -> float:
    """Return the configured child-process heartbeat interval.

    ``LEAF_PROGRESS_HEARTBEAT_SECONDS`` is intentionally operational and does
    not become part of experiment provenance because it cannot affect results.
    """

    env = os.environ if environ is None else environ
    raw = env.get("LEAF_PROGRESS_HEARTBEAT_SECONDS")
    if raw is None or not raw.strip():
        return DEFAULT_HEARTBEAT_SECONDS
    value = float(raw)
    if value <= 0:
        raise ValueError("LEAF_PROGRESS_HEARTBEAT_SECONDS must be > 0")
    return value




class RepeatedConsoleSetupFilter:
    """Suppress repeated research-setup chatter while preserving complete log files.

    The first complete Plant override block and Round override block are shown.
    Later byte-for-byte identical blocks are hidden from the interactive console.
    Likewise, repeated sample-0 Round/Wisp environment lines are shown once per
    distinct value. If any setup text changes later in the same orchestrator run,
    the changed block/line is shown rather than hidden.
    """

    _BLOCK_END = {
        "PLANT EXPERIMENT OVERRIDES": "All unspecified Plant properties canonical.",
        "ROUND EXPERIMENT OVERRIDES": "All unspecified Round effects canonical.",
    }
    _SINGLE_PREFIXES = (
        "resolved research environment sample 0 Round deck=",
        "resolved research environment sample 0 Wisp deck=",
    )

    def __init__(self) -> None:
        self._seen_blocks: set[tuple[str, ...]] = set()
        self._seen_single_lines: set[str] = set()
        self._buffer: list[str] | None = None
        self._block_end: str | None = None

    def feed(self, line: str) -> list[str]:
        stripped = line.rstrip("\r\n")

        if self._buffer is not None:
            self._buffer.append(line)
            if stripped == self._block_end:
                block = tuple(self._buffer)
                self._buffer = None
                self._block_end = None
                if block in self._seen_blocks:
                    return []
                self._seen_blocks.add(block)
                return list(block)
            return []

        if stripped in self._BLOCK_END:
            self._buffer = [line]
            self._block_end = self._BLOCK_END[stripped]
            return []

        if stripped.startswith(self._SINGLE_PREFIXES):
            if stripped in self._seen_single_lines:
                return []
            self._seen_single_lines.add(stripped)
            return [line]

        return [line]

    def flush(self) -> list[str]:
        """Return an unterminated buffered block rather than losing output."""
        if self._buffer is None:
            return []
        buffered = self._buffer
        self._buffer = None
        self._block_end = None
        return buffered


@dataclass(frozen=True)
class StepTiming:
    label: str
    category: str
    seconds: float
    skipped: bool


class ExperimentTimer:
    """Track a known sequence of experiment steps and estimate completion.

    Remaining-time estimates prefer the mean duration of the same step
    category (for example, training vs evaluation).  Before a category has a
    measurement, the mean of all measured non-skipped steps is used.  Resume
    skips never enter the averages.
    """

    def __init__(
        self,
        planned_categories: Sequence[str],
        *,
        planned_skips: Sequence[bool] | None = None,
        now_monotonic: Callable[[], float] = time.monotonic,
        now_datetime: Callable[[], datetime] = datetime.now,
        printer: Callable[..., None] = print,
    ) -> None:
        if not planned_categories:
            raise ValueError("planned_categories must not be empty")
        self._planned_categories = tuple(planned_categories)
        if planned_skips is None:
            self._planned_skips = tuple(False for _ in self._planned_categories)
        else:
            if len(planned_skips) != len(self._planned_categories):
                raise ValueError("planned_skips must match planned_categories length")
            self._planned_skips = tuple(bool(value) for value in planned_skips)
        self._now_monotonic = now_monotonic
        self._now_datetime = now_datetime
        self._print = printer
        self._experiment_started = now_monotonic()
        self._step_started: float | None = None
        self._step_label: str | None = None
        self._step_category: str | None = None
        self._completed: list[StepTiming] = []

    @property
    def total_steps(self) -> int:
        return len(self._planned_categories)

    @property
    def completed_steps(self) -> int:
        return len(self._completed)

    def start(self, label: str, category: str) -> None:
        if self._step_started is not None:
            raise RuntimeError("cannot start a new timed step before completing the current step")
        expected_index = len(self._completed)
        if expected_index >= self.total_steps:
            raise RuntimeError("all planned timed steps are already complete")
        expected_category = self._planned_categories[expected_index]
        if category != expected_category:
            raise ValueError(
                f"timing category mismatch at step {expected_index + 1}: "
                f"expected {expected_category!r}, got {category!r}"
            )

        self._step_started = self._now_monotonic()
        self._step_label = label
        self._step_category = category
        clock = self._now_datetime()

        self._emit("")
        self._emit("================================================================")
        self._emit(f"Run {expected_index + 1} of {self.total_steps} starting: {label}")
        self._emit(f"Clock: {clock.strftime('%Y-%m-%d %I:%M:%S %p')}")
        estimate = self._estimate_remaining(include_current=True)
        remaining_work = self._remaining_work_count(include_current=True)
        if remaining_work == 0:
            self._emit("Estimated overall remaining: no unfinished work")
        elif estimate is None:
            self._emit(
                f"Estimated overall remaining: waiting for a measured comparable run "
                f"({remaining_work} unfinished operation(s))"
            )
        else:
            eta = clock + timedelta(seconds=estimate)
            self._emit(
                f"Estimated overall remaining: {format_duration(estimate)}; "
                f"estimated experiment finish {eta.strftime('%a %b %d %I:%M %p')} "
                f"({remaining_work} unfinished operation(s))"
            )
        self._emit("================================================================")

    def complete(self, *, skipped: bool = False) -> StepTiming:
        if self._step_started is None or self._step_label is None or self._step_category is None:
            raise RuntimeError("no timed step is currently active")
        now = self._now_monotonic()
        seconds = max(0.0, now - self._step_started)
        timing = StepTiming(self._step_label, self._step_category, seconds, skipped)
        self._completed.append(timing)
        self._step_started = None
        self._step_label = None
        self._step_category = None

        elapsed = max(0.0, now - self._experiment_started)
        estimate = self._estimate_remaining(include_current=False)
        if skipped:
            status = "SKIPPED"
        else:
            status = f"took {format_duration(seconds)}"

        self._emit(
            f"RUN_COMPLETE {self.completed_steps}/{self.total_steps} {status}; "
            f"elapsed={format_duration(elapsed)}"
        )

        remaining_work = self._remaining_work_count(include_current=False)
        progress_message = (
            f"OVERALL_PROGRESS runs={self.completed_steps}/{self.total_steps} "
            f"elapsed={format_duration(elapsed)} remainingWork={remaining_work}"
        )
        if remaining_work == 0:
            progress_message += "; estimatedExperimentFinish=complete"
        elif estimate is None:
            progress_message += "; estRemaining=estimating; estimatedExperimentFinish=estimating"
        else:
            eta = self._now_datetime() + timedelta(seconds=estimate)
            progress_message += (
                f"; estRemaining={format_duration(estimate)}; "
                f"estimatedExperimentFinish={eta.strftime('%a %b %d %I:%M %p')}"
            )
        self._emit(progress_message)
        return timing

    def finish(self, description: str = "Experiment phase complete") -> None:
        elapsed = max(0.0, self._now_monotonic() - self._experiment_started)
        self._emit("")
        self._emit("================================================================")
        self._emit(f"{description}; elapsed={format_duration(elapsed)}")
        self._emit("================================================================")

    def _estimate_remaining(self, *, include_current: bool) -> float | None:
        measured = [item for item in self._completed if not item.skipped]
        remaining_indices = self._remaining_indices(include_current=include_current)
        if not remaining_indices:
            return 0.0
        if not measured:
            return None

        all_average = sum(item.seconds for item in measured) / len(measured)
        category_averages: dict[str, float] = {}
        for category in set(item.category for item in measured):
            values = [item.seconds for item in measured if item.category == category]
            category_averages[category] = sum(values) / len(values)

        family_averages: dict[str, float] = {}
        for family in set(self._category_family(item.category) for item in measured):
            values = [
                item.seconds
                for item in measured
                if self._category_family(item.category) == family
            ]
            family_averages[family] = sum(values) / len(values)

        total = 0.0
        for index in remaining_indices:
            category = self._planned_categories[index]
            family = self._category_family(category)
            total += category_averages.get(
                category, family_averages.get(family, all_average)
            )
        return total

    def _remaining_indices(self, *, include_current: bool) -> list[int]:
        # While a step is active, len(completed) is the current step's index.
        # After complete(), len(completed) is already the next step's index.
        start_index = len(self._completed)
        return [
            index
            for index in range(start_index, self.total_steps)
            if not self._planned_skips[index]
        ]

    def _remaining_work_count(self, *, include_current: bool) -> int:
        return len(self._remaining_indices(include_current=include_current))

    @staticmethod
    def _category_family(category: str) -> str:
        # Market orchestration uses categories such as train-2p/eval-4p.
        # The exact player-count category is preferred; the family gives a
        # sensible fallback before that exact category has a timing sample.
        return category.split("-", 1)[0]

    def _emit(self, message: str) -> None:
        self._print(message, flush=True)


def run_streaming_process(
    command: Sequence[str],
    *,
    cwd: Path,
    log: TextIO,
    heartbeat_interval: float | None = None,
    printer: Callable[..., None] = print,
    console_filter: RepeatedConsoleSetupFilter | None = None,
) -> None:
    """Run a child process while streaming output and emitting silence heartbeats.

    Reading directly with ``for line in process.stdout`` can block forever while
    a quiet Gradle/Kotlin stage is still healthy.  A reader thread feeds a queue
    while the main thread wakes periodically to print a heartbeat.  This keeps
    both an interactive terminal and ``|& tee ...`` visibly alive.
    """

    interval = heartbeat_seconds() if heartbeat_interval is None else heartbeat_interval
    if interval <= 0:
        raise ValueError("heartbeat_interval must be > 0")

    process = subprocess.Popen(
        list(command),
        cwd=cwd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )
    assert process.stdout is not None

    queue: Queue[str | None] = Queue()

    def reader() -> None:
        try:
            for line in process.stdout:
                queue.put(line)
        finally:
            queue.put(None)

    thread = Thread(target=reader, name="leaf-process-output-reader", daemon=True)
    thread.start()
    started = time.monotonic()
    command_name = Path(str(command[0])).name if command else "process"
    stream_closed = False

    while not stream_closed:
        try:
            item = queue.get(timeout=interval)
        except Empty:
            elapsed = max(0.0, time.monotonic() - started)
            printer(
                f"PROCESS_PROGRESS command={command_name} elapsed={format_duration(elapsed)} "
                f"clock={datetime.now().strftime('%I:%M:%S %p')} (child process still running)",
                flush=True,
            )
            continue

        if item is None:
            stream_closed = True
        else:
            # Always preserve the complete child output in the per-run log.
            log.write(item)
            log.flush()
            visible = [item] if console_filter is None else console_filter.feed(item)
            for visible_line in visible:
                printer(visible_line, end="", flush=True)

    if console_filter is not None:
        for visible_line in console_filter.flush():
            printer(visible_line, end="", flush=True)

    return_code = process.wait()
    thread.join(timeout=1.0)
    elapsed = max(0.0, time.monotonic() - started)
    printer(
        f"PROCESS_COMPLETE command={command_name} elapsed={format_duration(elapsed)} exit={return_code}",
        flush=True,
    )
    if return_code != 0:
        raise subprocess.CalledProcessError(return_code, list(command))

@dataclass(frozen=True)
class ExperimentStep:
    """One orchestration-level unit of work with a timing category.

    New research orchestrators should execute their train/evaluate/archive work
    through :class:`ExperimentPlanRunner` rather than hand-rolling timing.
    This makes wall-clock progress/ETA an automatic property of the common
    orchestration layer rather than an optional feature of individual scripts.
    """

    label: str
    category: str
    action: Callable[[], None]
    skip: bool = False


class ExperimentPlanRunner:
    """Execute an experiment plan with mandatory shared timing/progress output.

    This is intentionally above the game engine.  The game engine knows how to
    run one game but cannot know how many training/evaluation operations remain,
    so it cannot estimate an experiment ETA.  This runner is the highest common
    layer that owns the full sequence of long-running operations.
    """

    def __init__(
        self,
        steps: Sequence[ExperimentStep],
        *,
        printer: Callable[..., None] = print,
        now_monotonic: Callable[[], float] = time.monotonic,
        now_datetime: Callable[[], datetime] = datetime.now,
    ) -> None:
        if not steps:
            raise ValueError("steps must not be empty")
        self.steps = tuple(steps)
        self.printer = printer
        self.timer = ExperimentTimer(
            tuple(step.category for step in self.steps),
            planned_skips=tuple(step.skip for step in self.steps),
            printer=printer,
            now_monotonic=now_monotonic,
            now_datetime=now_datetime,
        )

    def run(self, *, description: str = "Experiment complete") -> None:
        for step in self.steps:
            self.timer.start(step.label, step.category)
            if step.skip:
                self.printer("Already complete; resume marker is present.", flush=True)
            else:
                step.action()
            self.timer.complete(skipped=step.skip)
        self.timer.finish(description)
