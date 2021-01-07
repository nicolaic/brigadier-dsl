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
import com.mojang.brigadier.arguments.BoolArgumentType.bool
import com.mojang.brigadier.arguments.BoolArgumentType.getBool
import com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg
import com.mojang.brigadier.arguments.DoubleArgumentType.getDouble
import com.mojang.brigadier.arguments.FloatArgumentType.floatArg
import com.mojang.brigadier.arguments.FloatArgumentType.getFloat
import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.LongArgumentType.getLong
import com.mojang.brigadier.arguments.LongArgumentType.longArg
import com.mojang.brigadier.arguments.StringArgumentType.*
import com.mojang.brigadier.context.CommandContext

private fun <T : Any> arg(
    name: String, type: ArgumentType<T>,
    getter: (CommandContext<Any>, String) -> T
) = RequiredArgument(name, type, getter)

fun boolean(name: String) = arg(name, bool(), ::getBool)

fun int(name: String, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE) = arg(name, integer(min, max), ::getInteger)
fun long(name: String, min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE) = arg(name, longArg(min, max), ::getLong)

fun float(name: String, min: Float = Float.MIN_VALUE, max: Float = Float.MAX_VALUE) =
    arg(name, floatArg(min, max), ::getFloat)

fun double(name: String, min: Double = Double.MIN_VALUE, max: Double = Double.MAX_VALUE) =
    arg(name, doubleArg(min, max), ::getDouble)

fun word(name: String) = arg(name, word(), ::getString)
fun string(name: String) = arg(name, string(), ::getString)
fun greedyString(name: String) = arg(name, greedyString(), ::getString)