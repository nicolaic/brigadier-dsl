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
import dev.nicolai.brigadier.OptionalArgument
import dev.nicolai.brigadier.RequiredArgument
import com.mojang.brigadier.Command as BrigadierCommand

sealed class DslCommandTree<S, A : ArgumentBuilder<S, A>>(
    private val contextRef: ContextRef<S>
) {
    private var inlineNode: DslCommandTree<S, *> = this

    private var command: BrigadierCommand<S>? = null

    private val children = mutableListOf<DslCommandTree<S, out ArgumentBuilder<S, *>>>()
    private val optionalArguments = mutableListOf<ArgumentDslCommandNode<S, *, *>>()

    private val subcommands = mutableListOf<Command<S>>()

    fun executes(command: BrigadierCommand<S>) {
        inlineNode.command = command
    }

    fun runs(command: (S) -> Unit) {
        executes { context ->
            command(context.source)
            SINGLE_SUCCESS
        }
    }

    fun addChild(node: DslCommandTree<S, *>) {
        inlineNode.children += node
    }

    fun literal(
        literal: String,
        apply: (LiteralArgumentBuilder<S>.() -> Unit)?
    ): LiteralDslCommandNode<S> {
        check(inlineNode.optionalArguments.isEmpty()) { "You cannot add literals after optional inline arguments" }
        return LiteralDslCommandNode(literal, apply, contextRef).also(inlineNode::addChild)
    }

    fun <T, V> argument(
        argument: RequiredArgument<S, T, V>,
        apply: (RequiredArgumentBuilder<S, T>.() -> Unit)?
    ): ArgumentDslCommandNode<S, T, V> {
        check(inlineNode.optionalArguments.isEmpty()) { "You cannot add nesting arguments after optional inline arguments" }
        return ArgumentDslCommandNode(argument, apply, contextRef).also(inlineNode::addChild)
    }

    fun <T, V> inlineArgument(
        argument: CommandArgument<S, T, V>
    ): ArgumentDslCommandNode<S, T, V> {
        return ArgumentDslCommandNode(argument, null, contextRef).also {
            if (argument is OptionalArgument<S, *, *, *>) {
                check(inlineNode.command == null) { "You cannot add optional inline arguments after the command has been set" }
                inlineNode.optionalArguments += it
            } else {
                check(inlineNode.optionalArguments.isEmpty()) { "You cannot add required inline arguments after optional inline arguments" }
                inlineNode.addChild(it)
                inlineNode = it
            }
        }
    }

    fun subcommands(vararg commands: Command<S>) {
        check(children.isEmpty()) {
            "Subcommands is not scoped to inline arguments, and therefore" +
                    "must be called before any arguments are registered"
        }

        subcommands += commands
    }

    protected abstract fun buildNode(): A

    fun buildTree(): A {
        val node = buildNode()

        check(children.isNotEmpty() || command != null) {
            "A node must either have a command or at least one child"
        }

        command?.let { command ->
            node.executes { context ->
                contextRef.context = context
                command.run(context)
            }

            buildOptionalArgumentsToNode(node, command)
        }

        subcommands
            .map(Command<S>::buildLiteral)
            .forEach(node::then)

        children
            .map { it.buildTree() }
            .forEach(node::then)

        return node
    }

    private fun buildOptionalArgumentsToNode(node: A, command: BrigadierCommand<S>) {
        if (optionalArguments.isEmpty()) return

        optionalArguments.forEach { it.executes(command) }

        node.then(optionalArguments
            .reduceRight { arg, acc -> arg.also { it.addChild(acc) } }
            .buildTree())
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