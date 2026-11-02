#!/usr/bin/env bash
set -euo pipefail
python3 bin/a03_verify_boundaries.py \
  --approve-path src/main/kotlin/dugsolutions/leaf/v35/effect/DefaultGameEffectExecutor.kt \
  --approve-path src/main/kotlin/dugsolutions/leaf/v35/effect/GameEffect.kt \
  --approve-path src/main/kotlin/dugsolutions/leaf/v35/effect/GameEffectConverter.kt \
  --approve-path src/main/kotlin/dugsolutions/leaf/v35/effect/GameEffectDecisionRequirements.kt \
  --approve-path src/main/kotlin/dugsolutions/leaf/v35/effect/handler/DieValueEffectHandler.kt \
  --reason 'B03 explicitly retires obsolete non-canonical Sunlight Raise plus three mechanic and its input mappings' \
  "$@"
