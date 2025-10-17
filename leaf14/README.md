# Leaf14

A rework of the Leaf game project, starting with core domain models and building up the system incrementally.

## Project Structure

This is a fresh start of the Leaf game project, beginning with the core domain model `FlourishType` and following the same architectural patterns as the original project.

## Getting Started

### Prerequisites
- Java 11 or higher
- Gradle 8.13

### Building the Project
```bash
./gradlew build
```

### Running Tests
```bash
./gradlew test
```

## Current Implementation

- **FlourishType**: Core domain enum representing the different types of flourish cards in the game
  - NONE, SEEDLING, ROOT, CANOPY, VINE, FLOWER, BLOOM

## Development Approach

This project follows the same development patterns as the original Leaf project:
- Kotlin with JVM target
- JUnit 5 for testing
- MockK for mocking
- Compose for UI components
- Koin for dependency injection

## Next Steps

The project is set up to incrementally add components from the original Leaf project, starting with core domain models and building up the complete game system.