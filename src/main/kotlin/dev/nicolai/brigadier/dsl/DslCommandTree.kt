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

sealed class DslCommandTree<S, A : ArgumentBuilder<S, out A>>(
    private val contextRef: ContextRef<S>
) {
    private var command: ((CommandContext<S>) -> Int)? = null

    private val children = mutableListOf<DslCommandTree<S, out ArgumentBuilder<S, *>>>()
    private val subcommands = mutableListOf<Command<S>>()

    fun executes(command: (CommandContext<S>) -> Int) {
        this.command = command
    }

    fun runs(command: (S) -> Unit) {
        executes { context ->
            command(context.source)
            SINGLE_SUCCESS
        }
    }

    fun literal(
        literal: String,
        apply: (LiteralArgumentBuilder<S>.() -> Unit)?
    ): LiteralDslCommandNode<S> {
        return LiteralDslCommandNode(literal, apply, contextRef).also { children += it }
    }

    fun <T, V> argument(
        argument: CommandArgument<S, T, V>,
        apply: (RequiredArgumentBuilder<S, T>.() -> Unit)?
    ): ArgumentDslCommandNode<S, T, V> {
        return ArgumentDslCommandNode(argument, apply, contextRef).also { children += it }
    }

    fun subcommands(vararg commands: Command<S>) {
        subcommands += commands
    }

    abstract fun buildNode(): A

    fun buildTree(): A {
        val node = buildNode()

        command?.let { command ->
            node.executes { context ->
                contextRef.context = context
                command(context)
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
    private val apply: (LiteralArgumentBuilder<S>.() -> Unit)?,
    contextRef: ContextRef<S>
) : DslCommandTree<S, LiteralArgumentBuilder<S>>(contextRef) {
    override fun buildNode(): LiteralArgumentBuilder<S> =
        LiteralArgumentBuilder.literal<S>(literal).also { apply?.invoke(it) }
}

class ArgumentDslCommandNode<S, T, V>(
    private val argument: CommandArgument<S, T, V>,
    private val apply: (RequiredArgumentBuilder<S, T>.() -> Unit)?,
    contextRef: ContextRef<S>
) : DslCommandTree<S, RequiredArgumentBuilder<S, T>>(contextRef) {

    val getter: () -> V = { argument.getValue(contextRef.context) }

    override fun buildNode() = argument.also { apply?.let(it::apply) }.buildArgument()
}

class ContextRef<S> {
    lateinit var context: CommandContext<S>
}