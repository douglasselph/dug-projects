# Cursor IDE Setup Guide for Leaf Project

## Quick Fix for "No tests" Error

If you're getting "No tests" when right-clicking on test files in Cursor, follow these steps:

### 1. Install Required Extensions
Cursor needs specific extensions to work with Kotlin and Gradle projects. Install these extensions:

1. **Extension Pack for Java** (`vscjava.vscode-java-pack`)
2. **Kotlin** (`fwcd.kotlin`)
3. **Gradle for Java** (`vscjava.vscode-gradle`)

### 2. Reload Cursor
After installing extensions, reload Cursor:
- `Ctrl+Shift+P` → "Developer: Reload Window"

### 3. Refresh Gradle Project
```bash
./gradlew clean build --refresh-dependencies
```

### 4. Force Java Language Server Restart
- `Ctrl+Shift+P` → "Java: Restart Language Server"

## Alternative: Use Command Line

If Cursor still doesn't recognize tests, you can run them via command line:

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "dugsolutions.leaf.player.PlayerTest"

# Run specific test method
./gradlew test --tests "dugsolutions.leaf.player.PlayerTest.pipModifierAdd_addsValueToList"

# Run with detailed output
./gradlew test --info
```

## Cursor-Specific Issues

### Problem: "No tests" when right-clicking
**Solution**: This is a common issue with Cursor and Kotlin projects. The Java Language Server needs to be properly configured.

### Problem: Tests not showing in Test Explorer
**Solution**: 
1. Install the "Extension Pack for Java" extension
2. Restart the Java Language Server
3. Wait for the project to be fully indexed

### Problem: Gradle tasks not recognized
**Solution**:
1. Install the "Gradle for Java" extension
2. Use the provided tasks in `.vscode/tasks.json`
3. Or run Gradle commands directly in the terminal

## Using VS Code Tasks in Cursor

You can use the provided tasks to run tests:

1. `Ctrl+Shift+P` → "Tasks: Run Task"
2. Select one of:
   - "Run Tests" - runs all tests
   - "Run Tests with Info" - runs tests with detailed output
   - "Clean and Build" - cleans and rebuilds the project

## Manual Test Verification

To verify tests are working:

```bash
# Check if tests compile
./gradlew compileTestKotlin

# Run a simple test
./gradlew test --tests "dugsolutions.leaf.player.PlayerTest.pipModifierAdd_addsValueToList"

# Check test output
./gradlew test --info
```

## Troubleshooting Steps

1. **Check Extensions**: Ensure Java and Kotlin extensions are installed
2. **Restart Language Server**: `Ctrl+Shift+P` → "Java: Restart Language Server"
3. **Reload Window**: `Ctrl+Shift+P` → "Developer: Reload Window"
4. **Clean Build**: `./gradlew clean build`
5. **Check Java Version**: Ensure you're using Java 11 or 17
6. **Verify Gradle**: `./gradlew --version`

## Success Indicators

- Test files show green "run" buttons in the editor
- Test Explorer panel shows test classes and methods
- Right-click on test files shows "Run Test" option
- Gradle tasks execute successfully

## Fallback Solution

If Cursor continues to have issues with test recognition, you can:

1. **Use Command Line**: All tests work perfectly via Gradle command line
2. **Use Android Studio**: Your original IDE setup works fine
3. **Use VS Code**: The configuration files are compatible with regular VS Code

## Project Status

✅ **Tests compile correctly**  
✅ **Tests run successfully via Gradle**  
✅ **Project builds without errors**  
✅ **All dependencies are properly configured**  

The issue is specifically with Cursor's test recognition, not with your project setup. 