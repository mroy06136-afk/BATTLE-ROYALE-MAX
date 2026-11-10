package com.example.ui.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ScopeType
import com.example.model.Weapon
import com.example.ui.theme.*

@Composable
fun WeaponArmoryDialog(
    onDismiss: () -> Unit
) {
    val weapons = remember {
        listOf(
            Weapon.createAK47(),
            Weapon.createM4A1(),
            Weapon.createMP40(),
            Weapon.createAWM(),
            Weapon.createSPAS12(),
            Weapon.createDesertEagle(),
            Weapon.createGrenade()
        )
    }
    var selectedWeapon by remember { mutableStateOf(weapons.first()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            shape = RoundedCornerShape(16.dp),
            color = BR_DarkBg,
            border = androidx.compose.foundation.BorderStroke(2.dp, BR_Cyan)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "WEAPONS ARMORY & UPGRADES",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Row(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                    // Left Weapon List
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(weapons) { w ->
                            val isSel = (w.id == selectedWeapon.id)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedWeapon = w }
                                    .border(
                                        1.5.dp,
                                        if (isSel) BR_Cyan else BR_Border,
                                        RoundedCornerShape(8.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) Color(0xFF1F2436) else BR_Surface
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(w.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${w.category.name.replace("_", " ")} | ${w.ammoType.label}", color = BR_TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right Weapon Stats & Upgrade
                    Card(
                        modifier = Modifier.weight(1.3f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = BR_Surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    selectedWeapon.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    "Caliber: ${selectedWeapon.ammoType.label} | Mag Capacity: ${selectedWeapon.magazineSize} Rounds",
                                    fontSize = 12.sp,
                                    color = BR_Orange
                                )

                                Divider(color = BR_Border, modifier = Modifier.padding(vertical = 12.dp))

                                WeaponStatBar(label = "DAMAGE", value = selectedWeapon.baseDamage / 150f, display = "${selectedWeapon.baseDamage.toInt()}", color = BR_Red)
                                WeaponStatBar(label = "RANGE", value = selectedWeapon.range / 500f, display = "${selectedWeapon.range.toInt()}m", color = BR_Cyan)
                                WeaponStatBar(label = "FIRE RATE", value = (1500f - selectedWeapon.fireIntervalMs.toFloat()) / 1500f, display = "${(1000L / selectedWeapon.fireIntervalMs)} rps", color = BR_Gold)
                                WeaponStatBar(label = "RELOAD SPEED", value = (3000f - selectedWeapon.reloadTimeMs.toFloat()) / 3000f, display = "${selectedWeapon.reloadTimeMs / 1000.0}s", color = Color(0xFF00E676))

                                Spacer(modifier = Modifier.height(12.dp))

                                Text("EQUIPPED OPTICS & ATTACHMENTS:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BR_TextSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(color = Color(0xFF1E293B), shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            "Optic: ${selectedWeapon.equippedScope.label}",
                                            fontSize = 11.sp,
                                            color = BR_Cyan,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(color = Color(0xFF1E293B), shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            "Muzzle: Compensator Lv2",
                                            fontSize = 11.sp,
                                            color = BR_Gold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = BR_CyanDark),
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Text("READY FOR DEPLOYMENT", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeaponStatBar(label: String, value: Float, display: String, color: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 11.sp, color = BR_TextSecondary, fontWeight = FontWeight.SemiBold)
            Text(display, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
            trackColor = Color(0x33FFFFFF)
        )
    }
}
