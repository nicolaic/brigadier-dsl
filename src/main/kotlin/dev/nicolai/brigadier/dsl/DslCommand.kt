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

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import dev.nicolai.brigadier.Command
import dev.nicolai.brigadier.CommandArgument
import dev.nicolai.brigadier.RequiredArgument
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

open class DslCommand<S>(
    private val literal: String,
    private val apply: (LiteralArgumentBuilder<S>.() -> Unit)? = null,
    private val builderBlock: (DslCommandBuilder<S>.() -> Unit)
) : Command<S> {

    constructor(literal: String, builderBlock: DslCommandBuilder<S>.() -> Unit) : this(literal, null, builderBlock)

    override fun buildLiteral(): LiteralArgumentBuilder<S> {
        val dslNode = LiteralDslCommandNode(literal, apply, ContextRef())

        val builder = DslCommandBuilder(dslNode)
        builder.apply(builderBlock)

        return dslNode.buildTree()
    }
}

class DslCommandBuilder<S>(private val dslTree: DslCommandTree<S, *>) {

    fun executes(command: (CommandContext<S>) -> Int) {
        dslTree.executes(command)
    }

    infix fun runs(command: (S) -> Unit) {
        dslTree.runs(command)
    }

    fun literal(
        literal: String,
        apply: (LiteralArgumentBuilder<S>.() -> Unit)? = null,
        block: (DslCommandBuilder<S>.() -> Unit)? = null
    ): DslCommandBuilder<S> {
        val literalNode = dslTree.literal(literal, apply)

        return DslCommandBuilder(literalNode).also { block?.invoke(it) }
    }

    fun literals(
        vararg literals: String,
        block: (DslCommandBuilder<S>.() -> Unit)? = null
    ): DslCommandBuilder<S> {
        val literalNode = literals.fold(dslTree) { node, literal -> node.literal(literal, null) }

        return DslCommandBuilder(literalNode).also { block?.invoke(it) }
    }

    fun <T> arg(
        arg: RequiredArgument<S, T>,
        apply: (RequiredArgumentBuilder<S, T>.() -> Unit)? = null,
        block: DslCommandBuilder<S>.(() -> T) -> Unit
    ) {
        val argNode = dslTree.argument(arg, apply)

        val builder = DslCommandBuilder(argNode)
        block(builder, argNode.getter)
    }

    operator fun <T, V> CommandArgument<S, T, V>.provideDelegate(
        thisRef: Any?,
        property: KProperty<*>
    ): ReadOnlyProperty<Any?, V> {
        val argument = dslTree.inlineArgument(this)
        return ReadOnlyProperty { _, _ -> argument.getter() }
    }

    fun subcommands(vararg commands: Command<S>) {
        dslTree.subcommands(*commands)
    }
}

fun <S> command(
    literal: String,
    apply: (LiteralArgumentBuilder<S>.() -> Unit)? = null,
    block: DslCommandBuilder<S>.() -> Unit
) = DslCommand(literal, apply, block)