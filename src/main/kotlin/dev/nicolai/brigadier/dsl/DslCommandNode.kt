/*
 * Copyright 2020-present Nicolai Christophersen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.nicolai.brigadier.dsl

import com.mojang.brigadier.Command.SINGLE_SUCCESS
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import dev.nicolai.brigadier.Command
import dev.nicolai.brigadier.CommandArgument

sealed class DslCommandNode<S, A : ArgumentBuilder<S, out A>>(
    private val contextRef: ContextRef<S>
) {
    private var executes: ((CommandContext<S>) -> Int)? = null

    private val children = mutableListOf<DslCommandNode<S, out ArgumentBuilder<S, *>>>()
    private val subcommands = mutableListOf<Command<S>>()

    fun executes(command: (CommandContext<S>) -> Int) {
        executes = command
    }

    fun runs(command: (S) -> Unit) {
        executes { context ->
            command(context.source)
            SINGLE_SUCCESS
        }
    }

    fun literal(literal: String): LiteralDslCommandNode<S> {
        return LiteralDslCommandNode(literal, contextRef).also { children += it }
    }

    fun <T> argument(argument: CommandArgument<S, T>): ArgumentDslCommandNode<S, T> {
        return ArgumentDslCommandNode(argument, contextRef).also { children += it }
    }

    fun subcommands(vararg commands: Command<S>) {
        subcommands += commands
    }

    abstract fun buildNode(): A

    fun buildTree(): A {
        val node = buildNode()

        executes?.let { executes ->
            node.executes { context ->
                contextRef.context = context
                executes.invoke(context)
            }
        }

        subcommands
            .map(Command<S>::buildLiteral)
            .forEach(node::then)

        children
            .map { it.buildTree() }
            .forEach(node::then)

        return node
    }
}

class LiteralDslCommandNode<S>(
    private val literal: String,
    contextRef: ContextRef<S>
) : DslCommandNode<S, LiteralArgumentBuilder<S>>(contextRef) {
    override fun buildNode() = LiteralArgumentBuilder.literal<S>(literal)
}

class ArgumentDslCommandNode<S, T>(
    private val argument: CommandArgument<S, T>,
    contextRef: ContextRef<S>
) : DslCommandNode<S, RequiredArgumentBuilder<S, out T>>(contextRef) {

    val getter: () -> T = { argument.getValue(contextRef.context) }

    override fun buildNode() = argument.buildArgument()
}

class ContextRef<S> {
    lateinit var context: CommandContext<S>
}