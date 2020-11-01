package io.github.bluepython508.conveniences

import net.minecraft.client.render.VertexConsumer

typealias Colour = Triple<Int, Int, Int>
typealias ColourAlpha = Pair<Colour, Int>

val Colour.red
    get() = first

val Colour.blue
    get() = second

val Colour.green
    get() = third

val ColourAlpha.alpha
    get() = second

val ColourAlpha.red
    get() = first.red

val ColourAlpha.blue
    get() = first.blue

val ColourAlpha.green
    get() = first.green

fun ColourAlpha(red: Int, blue: Int, green: Int, alpha: Int = 255): ColourAlpha = Colour(red, blue, green) to alpha

fun VertexConsumer.color(colour: ColourAlpha) = color(colour.red, colour.blue, colour.green, colour.alpha)

fun ColourAlpha.toInt(): Int = red shl 24 + blue shl 16 + green shl 8 + alpha