package com.duodeck.launcher.data

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

enum class Rarity(val label: String, val weight: Int, val a: Color, val b: Color) {
    COMMON("Common", 60, Color(0xFF90A4AE), Color(0xFFCFD8DC)),
    RARE("Rare", 28, Color(0xFF29B6F6), Color(0xFFB3E5FC)),
    EPIC("Epic", 10, Color(0xFFAB47BC), Color(0xFFE1BEE7)),
    LEGENDARY("Legendary", 2, Color(0xFFFFB300), Color(0xFFFFF59D)),
}

/** A collectible badge. Icons are plain emoji on a rarity-coloured medallion, so nothing is copied from anywhere. */
data class Badge(val id: String, val name: String, val glyph: String, val rarity: Rarity)

object Badges {
    private fun b(id: String, name: String, g: String, r: Rarity) = Badge(id, name, g, r)
    private val C = Rarity.COMMON
    private val R = Rarity.RARE
    private val E = Rarity.EPIC
    private val L = Rarity.LEGENDARY

    val all: List<Badge> = listOf(
        b("pad", "Game Pad", "🎮", C), b("cherry", "Cherry", "🍒", C), b("melon", "Watermelon", "🍉", C),
        b("donut", "Donut", "🍩", C), b("cat", "Arcade Cat", "🐱", C), b("pup", "Pixel Pup", "🐶", C),
        b("frog", "Hop Frog", "🐸", C), b("pizza", "Pizza Slice", "🍕", C), b("ice", "Ice Cream", "🍦", C),
        b("balloon", "Balloon", "🎈", C), b("bee", "Buzz Bee", "🐝", C), b("clover", "Clover", "🍀", C),
        b("note", "Music Note", "🎵", C), b("dice", "Lucky Dice", "🎲", C), b("shroom", "Spotty Shroom", "🍄", C),
        b("fox", "Sly Fox", "🦊", R), b("octo", "Octo Pal", "🐙", R), b("rocket", "Rocket", "🚀", R),
        b("moon", "Moon", "🌙", R), b("bloom", "Cherry Bloom", "🌸", R), b("guitar", "Rock Star", "🎸", R),
        b("puzzle", "Puzzle Piece", "🧩", R), b("target", "Bullseye", "🎯", R), b("bear", "Teddy", "🧸", R),
        b("invader", "Space Invader", "👾", R), b("joystick", "Joystick", "🕹️", R),
        b("rex", "Rex", "🦖", E), b("rainbow", "Rainbow", "🌈", E), b("fire", "Hot Streak", "🔥", E),
        b("ufo", "Flying Saucer", "🛸", E), b("crystal", "Crystal Ball", "🔮", E), b("bolt", "Lightning", "⚡", E),
        b("gem", "Diamond", "💎", L), b("crown", "Golden Crown", "👑", L), b("trophy", "Trophy", "🏆", L),
        b("star", "Super Star", "⭐", L),
    )

    val byId: Map<String, Badge> = all.associateBy { it.id }

    /** Rolls a random badge, weighted by rarity. */
    fun roll(rnd: Random = Random): Badge {
        val total = Rarity.values().sumOf { it.weight }
        var n = rnd.nextInt(total)
        val rarity = Rarity.values().first { n -= it.weight; n < 0 }
        return all.filter { it.rarity == rarity }.random(rnd)
    }

    const val CLAW_COST = 5
}
