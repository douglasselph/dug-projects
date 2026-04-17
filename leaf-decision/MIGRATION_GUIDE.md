# Migration Guide: Simplified Decision System

## Overview

This guide explains how to migrate from the complex `DecisionTaskQueue` system to the new simplified decision system.

## Key Benefits of the New System

1. **No Queue Complexity**: Only one decision at a time, no queuing
2. **Clear State**: Easy to see if waiting for a decision
3. **Simple Debugging**: Clear call stack, no hidden state
4. **No Race Conditions**: Only one decision active at a time
5. **Direct Communication**: No complex notification chains

## Migration Steps

### Step 1: Replace DecisionDirector

**Old System:**
```kotlin
class DecisionDirector(
    private val cardEffectBattleScoreFactory: CardEffectBattleScoreFactory,
    private val cardManager: CardManager,
    private val acquireCardEvaluator: AcquireCardEvaluator,
    private val acquireDieEvaluator: AcquireDieEvaluator,
    private val groveNearingTransition: GroveNearingTransition
) {
    lateinit var drawCountDecision: DecisionDrawCount
    // ... other decisions
    
    fun initialize(player: Player) {
        drawCountDecision = DecisionDrawCountBaseline()
        // ... other baseline decisions
    }
}
```

**New System:**
```kotlin
class SimpleDecisionManager(
    private val cardEffectBattleScoreFactory: CardEffectBattleScoreFactory,
    private val cardManager: CardManager,
    private val acquireCardEvaluator: AcquireCardEvaluator,
    private val acquireDieEvaluator: AcquireDieEvaluator,
    private val groveNearingTransition: GroveNearingTransition
) {
    lateinit var drawCountDecision: DecisionDrawCount
    // ... other decisions
    
    fun initialize(player: Player, useUI: Boolean = false) {
        if (useUI) {
            drawCountDecision = SimpleDecisionDrawCount()
            // ... other UI decisions
        } else {
            drawCountDecision = DecisionDrawCountBaseline()
            // ... other baseline decisions
        }
    }
}
```

### Step 2: Replace DecisionTaskQueue with SimpleDecisionHandler

**Old System:**
```kotlin
class DecisionDrawCountSuspend(
    monitor: DecisionMonitor,
    report: DecisionMonitorReport
) : DecisionDrawCount {
    private val taskQueue = DecisionTaskQueue<DecisionDrawCount.Result>(monitor, report)
    
    override suspend fun invoke(player: Player): DecisionDrawCount.Result {
        return taskQueue.waitForDecision(DecisionID.DRAW_COUNT(player))
    }
    
    fun provide(result: DecisionDrawCount.Result) {
        taskQueue.provideDecision(result)
    }
}
```

**New System:**
```kotlin
class SimpleDecisionDrawCount : DecisionDrawCount {
    private val handler = SimpleDecisionHandler<DecisionDrawCount.Result>()
    
    override suspend fun invoke(player: Player): DecisionDrawCount.Result {
        return handler.waitForDecision()
    }
    
    fun provide(result: DecisionDrawCount.Result) {
        handler.provideDecision(result)
    }
    
    fun isWaiting(): Boolean = handler.isWaiting()
}
```

### Step 3: Replace MainDecisions

**Old System:**
```kotlin
class MainDecisions(
    private val mainGameManager: MainGameManager,
    private val cardOperations: CardOperations,
    private val decisionMonitor: DecisionMonitor,
    private val decisionMonitorReport: DecisionMonitorReport,
    private val shouldAskTrashEffect: ShouldAskTrashEffect,
    private val decidingPlayerManager: DecidingPlayer
) {
    fun setup(player: Player) = with(player.decisionDirector) {
        decisionMonitor.subscribe { id -> applyDecisionId(player, id) }
        drawCountDecision = DecisionDrawCountSuspend(decisionMonitor, decisionMonitorReport)
        // ... other decisions
    }
    
    private fun applyDecisionId(player: Player, id: DecisionID?) {
        when (id) {
            is DecisionID.DRAW_COUNT -> {
                mainGameManager.resetData()
            }
            // ... other cases
        }
    }
}
```

**New System:**
```kotlin
class SimpleMainDecisions(
    private val mainGameManager: MainGameManager,
    private val cardOperations: CardOperations,
    private val shouldAskTrashEffect: ShouldAskTrashEffect,
    private val decidingPlayerManager: DecidingPlayer
) {
    fun setup(player: Player) {
        player.decisionDirector.initialize(player, useUI = true)
        shouldAskTrashEffect.askTrashOkay = false
    }
    
    fun onDrawCountChosen(player: Player, value: Int) {
        val drawCountDecision = player.decisionDirector.drawCountDecision
        if (drawCountDecision is SimpleDecisionDrawCount) {
            drawCountDecision.provide(DecisionDrawCount.Result(value))
        }
    }
}
```

## Key Differences

### 1. No Complex Notification System

**Old:** Complex notification chain through `DecisionMonitor` → `MainDecisions` → UI
**New:** Direct communication between decision handlers and UI

### 2. No Queue Management

**Old:** Complex queue system with multiple pending decisions
**New:** Only one decision at a time, simple state management

### 3. No Race Conditions

**Old:** Multiple decisions could be queued simultaneously
**New:** Only one decision active at a time, no queuing

### 4. Easier Debugging

**Old:** Complex state to track (queue, active task, notifications)
**New:** Simple state (waiting/not waiting, direct communication)

## Implementation Checklist

- [ ] Replace `DecisionDirector` with `SimpleDecisionManager`
- [ ] Replace `DecisionTaskQueue` with `SimpleDecisionHandler`
- [ ] Replace `DecisionDrawCountSuspend` with `SimpleDecisionDrawCount`
- [ ] Replace `MainDecisions` with `SimpleMainDecisions`
- [ ] Remove `DecisionMonitor` and `DecisionID` dependencies
- [ ] Update UI integration to use direct communication
- [ ] Test the new system thoroughly
- [ ] Remove old complex system files

## Testing the New System

```kotlin
// Test that decisions work correctly
suspend fun testDecisionFlow() {
    val decision = SimpleDecisionDrawCount()
    
    // Start decision in background
    val result = async { decision.invoke(player) }
    
    // Provide decision from UI
    decision.provide(DecisionDrawCount.Result(3))
    
    // Verify result
    assertEquals(3, result.await().count)
}
```

## Benefits After Migration

1. **Simpler Code**: No complex queue management
2. **Easier Debugging**: Clear call stack, no hidden state
3. **Better Performance**: No unnecessary queuing overhead
4. **More Reliable**: No race conditions or complex state management
5. **Easier Testing**: Direct communication, simple state
