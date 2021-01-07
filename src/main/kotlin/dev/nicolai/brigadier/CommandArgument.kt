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

package dev.nicolai.brigadier

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext

sealed class CommandArgument<S, T> {

    abstract fun getValue(context: CommandContext<S>): T

    abstract fun buildArgument(): RequiredArgumentBuilder<S, out T>
}

class RequiredArgument<S, T : Any>(
    private val name: String, private val type: ArgumentType<T>,
    private val getter: (CommandContext<S>, String) -> T
) : CommandArgument<S, T>() {
    override fun getValue(context: CommandContext<S>): T = getter(context, name)

    override fun buildArgument(): RequiredArgumentBuilder<S, T> {
        return RequiredArgumentBuilder.argument(name, type)
    }
}

fun <S, T : Any> RequiredArgument<S, T>.optional() = OptionalArgument(this)
fun <S, T : Any> RequiredArgument<S, T>.default(value: T) = DefaultArgument(this, value)

class OptionalArgument<S, T : Any>(
    private val argument: RequiredArgument<S, T>
) : CommandArgument<S, T?>() {

    override fun getValue(context: CommandContext<S>): T? = try {
        argument.getValue(context)
    } catch (e: IllegalArgumentException) {
        null
    }

    override fun buildArgument() = argument.buildArgument()
}

class DefaultArgument<S, T : Any>(
    private val argument: RequiredArgument<S, T>,
    private val default: T
) : CommandArgument<S, T>() {

    override fun getValue(context: CommandContext<S>): T = try {
        argument.getValue(context)
    } catch (e: IllegalArgumentException) {
        default
    }

    override fun buildArgument() = argument.buildArgument()
}