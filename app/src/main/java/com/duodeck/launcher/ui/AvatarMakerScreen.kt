package com.duodeck.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duodeck.launcher.LibraryViewModel
import com.duodeck.launcher.data.Avatar

private val Cats = listOf("Face", "Hair", "Eyes", "Brows", "Nose", "Mouth", "Extras", "Outfit")

/** Original Mii-style character creator (all parts are drawn in code; no Nintendo assets). */
@Composable
fun AvatarMakerScreen(vm: LibraryViewModel, modifier: Modifier = Modifier) {
    val duo = LocalDuo.current
    val user by vm.user.collectAsStateWithLifecycle()
    val a = user.avatar
    var cat by remember { mutableIntStateOf(0) }
    fun set(n: Avatar) = vm.setAvatar(n)

    DotBackground(modifier) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            ScreenHeader("Avatar Maker", vm)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AvatarView(a, Modifier.size(150.dp).clip(RoundedCornerShape(28.dp))
                        .border(2.dp, duo.stroke, RoundedCornerShape(28.dp)))
                    Column(Modifier.weight(1f)) {
                        Text("Name", color = duo.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        DuoField(a.name, { set(a.copy(name = it.take(14))) }, "Your name")
                        Spacer(Modifier.height(10.dp))
                        Pill("🎲 Randomise", true, { set(AvatarParts.random(a.name)) })
                    }
                }

                Spacer(Modifier.height(14.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Cats.size) { i -> Pill(Cats[i], cat == i, { cat = i }) }
                }
                Spacer(Modifier.height(10.dp))

                DuoCard {
                    when (cat) {
                        0 -> {
                            Styles("Head shape", AvatarParts.HEADS, a.head) { set(a.copy(head = it)) }
                            Swatches("Skin", AvatarParts.skins, a.skin) { set(a.copy(skin = it)) }
                            Toggle("Rosy cheeks", a.cheeks) { set(a.copy(cheeks = it)) }
                        }
                        1 -> {
                            Styles("Hairstyle", AvatarParts.HAIRS, a.hair, zeroLabel = "Bald") { set(a.copy(hair = it)) }
                            Swatches("Hair colour", AvatarParts.hairColors, a.hairColor) { set(a.copy(hairColor = it)) }
                        }
                        2 -> Styles("Eyes", AvatarParts.EYES, a.eyes) { set(a.copy(eyes = it)) }
                        3 -> {
                            Styles("Eyebrows", AvatarParts.BROWS, a.brows, zeroLabel = "None") { set(a.copy(brows = it)) }
                        }
                        4 -> Styles("Nose", AvatarParts.NOSES, a.nose) { set(a.copy(nose = it)) }
                        5 -> Styles("Mouth", AvatarParts.MOUTHS, a.mouth) { set(a.copy(mouth = it)) }
                        6 -> {
                            Styles("Glasses", AvatarParts.GLASSES, a.glasses, zeroLabel = "None") { set(a.copy(glasses = it)) }
                            Styles("Facial hair", AvatarParts.BEARDS, a.beard, zeroLabel = "None") { set(a.copy(beard = it)) }
                        }
                        else -> {
                            Swatches("Shirt", AvatarParts.shirts, a.shirt) { set(a.copy(shirt = it)) }
                            Swatches("Background", AvatarParts.backgrounds, a.bg) { set(a.copy(bg = it)) }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Saved automatically. Your avatar shows on the top screen.", color = duo.muted, fontSize = 12.sp)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Styles(title: String, count: Int, current: Int, zeroLabel: String? = null, onPick: (Int) -> Unit) {
    val duo = LocalDuo.current
    Text(title, color = duo.text, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(count) { i ->
            Pill(if (i == 0 && zeroLabel != null) zeroLabel else "${i + 1}", current == i, { onPick(i) })
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Swatches(title: String, colors: List<Color>, current: Int, onPick: (Int) -> Unit) {
    val duo = LocalDuo.current
    Text(title, color = duo.text, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(colors.size) { i ->
            Box(Modifier.size(34.dp).bounceClick { onPick(i) }.clip(CircleShape).background(colors[i])
                .border(if (current == i) 3.dp else 1.dp, if (current == i) duo.text else duo.stroke, CircleShape))
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Toggle(title: String, on: Boolean, onChange: (Boolean) -> Unit) {
    ToggleRow(title, "", on, onChange)
}
