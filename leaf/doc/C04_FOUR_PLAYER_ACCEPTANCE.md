# C04 four-player reviewed acceptance (V3-18)

The fixture in `BattleActionCoordinatorTest` mirrors the **actual four-player V3-18 board**: A 4/3/13; B 2/5/4; C 7/8/9; D 9/3/13 (TOP/MIDDLE/BOTTOM). All listed dice are physically placed in `BattleState.grid`; Round +1 VP and Draw D8 are checked against the coordinator's generated first-Main legal choices. The Human Baseline candidate scores are emitted as `C04_ENGINE_LEGAL` diagnostics.

**Scope limitation:** This is a legal **snapshot** with valid placements, not a validated chronological sequence from initial Rank-and-Place. Several starting rows are inconsistent with required descending initial dice and would need prior draws/effects. This does *not* yet certify a complete original human-review state or any Plant target selection. The earlier two-player adaptation remains separately diagnostic.

No numeric coefficients or mechanics were changed. Review the diagnostic records before interpreting preference agreement. The original author response preferred B (Draw D8, confidence 4); a mismatch is a signal for analysis, not automatically an assertion failure.

## Run and extract diagnostics

```sh
./gradlew test --tests '*BattleActionCoordinatorTest' --rerun-tasks --info 2>&1 | tee c04-coordinator-acceptance.log
grep 'C04_ENGINE_LEGAL|' c04-coordinator-acceptance.log
```

If Gradle prints captured standard output only in XML reports:

```sh
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
for p in Path('build/test-results/test').glob('TEST-*.xml'):
    root = ET.parse(p).getroot()
    for node in root.iter('system-out'):
        for line in (node.text or '').splitlines():
            if 'C04_ENGINE_LEGAL|' in line:
                print(line.strip())
PY
```

Send the resulting lines. No manual strategic review is required.
