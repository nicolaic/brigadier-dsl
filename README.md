[license-badge]: https://img.shields.io/github/license/nicolaic/brigadier-dsl.svg
[actions-badge]: https://github.com/nicolaic/brigadier-dsl/actions/workflows/gradle.yml/badge.svg
[actions-gradle]: https://github.com/nicolaic/brigadier-dsl/actions/workflows/gradle.yml

[brigadier-github]: https://github.com/Mojang/brigadier/

[wiki-motivation]: https://github.com/nicolaic/brigadier-dsl/wiki/Motivation

# Brigadier DSL [![License][license-badge]](/LICENSE) [![Kotlin CI with Gradle][actions-badge]][actions-gradle]

Brigadier DSL is a Kotlin library that adds a new fluent DSL on top of [Mojang's Brigadier][brigadier-github], that
makes it easier to write commands. For more information read the [Motivation][wiki-motivation] page.

### Features

- Fluent and easy to use Kotlin DSL
- Lexically scoped type-safe arguments
- Automatic and safe retrieval of values
- Optional and default value arguments
- Dynamic default values from command source

### Quickstart

##### Add the repository

```kotlin
repositories {
    mavenCentral()
}
```

##### Install the dependency

```kotlin
dependencies {
    implementation("dev.nicolai:brigadier-dsl:{current_version}")
}
```

##### Create and register a command

```kotlin
// Import command DSL function and argument shorthands
import dev.nicolai.brigadier.dsl.command
import dev.nicolai.brigadier.arguments.*

// Declare command with source of type MessageSender
val whisper = command<MessageSender>("whisper") {
    // Declare our recipient and message arguments
    val recipient by string("recipient")
    val message by greedyString("message")

    // Code to be run when the command is executed
    runs {
        // Access argument value as variables
        val feedback = "Sent '$message' to $recipient"

        // Source and context are implicitly available in the runs block
        source.sendFeedback(feedback)
    }
}

// Build the command literal and register it
dispatcher.register(whisper.buildLiteral())
```