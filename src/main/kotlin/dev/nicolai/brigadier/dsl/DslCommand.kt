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
import com.mojang.brigadier.context.CommandContext
import dev.nicolai.brigadier.Command
import dev.nicolai.brigadier.RequiredArgument

class DslCommand<S>(
    private val literal: String,
    private val builderBlock: (DslCommandBuilder<S>.() -> Unit)
) : Command<S> {

    override fun buildLiteral(): LiteralArgumentBuilder<S> {
        val dslNode = LiteralDslCommandNode<S>(literal, ContextRef())

        val builder = DslCommandBuilder(dslNode)
        builder.apply(builderBlock)

        return dslNode.buildTree()
    }
}

class DslCommandBuilder<S> internal constructor(private val dslNode: DslCommandNode<S, *>) {

    fun executes(command: (CommandContext<S>) -> Int) {
        dslNode.executes(command)
    }

    infix fun runs(command: (S) -> Unit) {
        dslNode.runs(command)
    }

    fun literal(literal: String, block: (DslCommandBuilder<S>.() -> Unit)? = null): DslCommandBuilder<S> {
        val literalNode = dslNode.literal(literal)

        return DslCommandBuilder(literalNode).also { block?.invoke(it) }
    }

    fun literals(vararg literals: String, block: (DslCommandBuilder<S>.() -> Unit)? = null): DslCommandBuilder<S> {
        val literalNode = literals.fold(dslNode) { node, literal -> node.literal(literal) }

        return DslCommandBuilder(literalNode).also { block?.invoke(it) }
    }

    fun <T> arg(arg: RequiredArgument<S, T>, block: DslCommandBuilder<S>.(() -> T) -> Unit) {
        val argNode = dslNode.argument(arg)

        val builder = DslCommandBuilder(argNode)
        block(builder, argNode.getter)
    }

    fun subcommands(vararg commands: Command<S>) {
        dslNode.subcommands(*commands)
    }
}

fun <S> command(literal: String, block: DslCommandBuilder<S>.() -> Unit) = DslCommand(literal, block)