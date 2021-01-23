[license-badge]: https://img.shields.io/github/license/nicolaic/brigadier-dsl.svg

[brigadier-github]: https://github.com/Mojang/brigadier/

[wiki-motivation]: [https://github.com/nicolaic/brigadier-dsl/wiki/moviation]

# Brigadier DSL [![License][license-badge]](/LICENSE)

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
    maven(url = "https://maven.pkg.github.com/nicolaic/brigadier-dsl")
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

val whisper = command<Any>("whisper") {
    // Declare our recipient and message arguments
    val recipient by string("recipient")
    val message by greedyString("message")

    // Code to be run when the command is executed
    runs {
        // Access argument value as variables
        println("Sent '$message' to $recipient")
    }
}

// Build the command literal and register it
dispatcher.register(whisper.buildLiteral())
```